package com.hit.fp.core

/*
 * ---------------------------------------------------------------------------
 * Transform.scala
 *
 * ADVANCED FUNCTIONAL PROGRAMMING TECHNIQUE 1 : A CUSTOM COMBINATOR.
 *
 * Transform is a custom function type, written from scratch, that knows how
 * to be combined with another Transform. It is deliberately NOT based on the
 * compose and andThen functions of the standard Function1 trait, because the
 * project document states that reusing Function1 is not considered a custom
 * combinator implementation.
 *
 * Three combinators are defined here:
 *   ~>      combines two transformations in sequence (a -> b -> c)
 *   zip     runs two transformations on the same input and pairs the results
 *   filterK keeps the transformed value only when a predicate accepts it
 *
 * The trait extends Serializable because instances of Transform are used
 * inside Spark transformations, and Spark ships the closures of its
 * transformations to the executors over the network.
 * ---------------------------------------------------------------------------
 */

/**
 * A composable transformation from a value of type `A` to a value of type `B`.
 *
 * The trait is generic, therefore any transformation of the pipeline can be
 * expressed with it, and every implementation immediately gains the three
 * combinators defined below.
 *
 * @tparam A type of the accepted value
 * @tparam B type of the produced value
 */
trait Transform[A, B] extends Serializable {

  /**
   * Applies the transformation to a single value.
   *
   * @param input value to transform
   * @return the transformed value
   */
  def run(input: A): B

  /**
   * Sequential combinator. Builds a new transformation that first runs this
   * transformation and then runs the given one on the produced value.
   *
   * @param next transformation to run on the result of this transformation
   * @tparam C type produced by the given transformation
   * @return the combined transformation
   */
  def ~>[C](next: Transform[B, C]): Transform[A, C] = {
    val self = this
    new Transform[A, C] {
      override def run(input: A): C = next.run(self.run(input))
    }
  }

  /**
   * Parallel combinator. Builds a new transformation that runs both this
   * transformation and the given one on the very same input, and pairs the
   * two produced values.
   *
   * @param other transformation to run on the same input
   * @tparam C type produced by the given transformation
   * @return the combined transformation producing a pair of results
   */
  def zip[C](other: Transform[A, C]): Transform[A, (B, C)] = {
    val self = this
    new Transform[A, (B, C)] {
      override def run(input: A): (B, C) = (self.run(input), other.run(input))
    }
  }

  /**
   * Filtering combinator. Builds a new transformation that produces the
   * transformed value wrapped in a Some when the predicate accepts it, and
   * None otherwise.
   *
   * @param predicate condition the produced value has to satisfy
   * @return the combined transformation producing an optional result
   */
  def filterK(predicate: B => Boolean): Transform[A, Option[B]] = {
    val self = this
    new Transform[A, Option[B]] {
      override def run(input: A): Option[B] = {
        val produced = self.run(input)
        if (predicate(produced)) Some(produced) else None
      }
    }
  }
}

/**
 * Companion object of [[Transform]], holding its constructors.
 */
object Transform {

  /**
   * Lifts a plain anonymous function into a combinable transformation.
   *
   * @param function function to lift
   * @tparam A type of the accepted value
   * @tparam B type of the produced value
   * @return a transformation that delegates to the given function
   */
  def lift[A, B](function: A => B): Transform[A, B] =
    new Transform[A, B] {
      override def run(input: A): B = function(input)
    }

  /**
   * Builds the transformation that returns its input unchanged. It is the
   * neutral element of the `~>` combinator, which makes [[chain]] total.
   *
   * @tparam A type of the accepted and produced value
   * @return the identity transformation
   */
  def identity[A]: Transform[A, A] =
    new Transform[A, A] {
      override def run(input: A): A = input
    }

  /**
   * Combines a whole list of transformations of the same type into a single
   * transformation, by folding the list with the `~>` combinator.
   *
   * @param steps transformations to combine, in the order of application
   * @tparam A type of the accepted and produced value
   * @return the combined transformation
   */
  def chain[A](steps: List[Transform[A, A]]): Transform[A, A] =
    steps.foldLeft(identity[A])((combined, step) => combined ~> step)
}
