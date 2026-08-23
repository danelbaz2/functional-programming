package com.hit.fp.core

/*
 * ---------------------------------------------------------------------------
 * Rng.scala
 *
 * A purely functional pseudo random number generator, used to build the
 * synthetic dataset of the project in a reproducible way.
 *
 * The generator of the standard library (scala.util.Random) is a mutable
 * object: calling nextInt twice on the same instance returns two different
 * values, which breaks referential transparency. The generator below is an
 * immutable value instead: every draw returns both the drawn number and the
 * next generator, so the caller threads the state explicitly and the very
 * same seed always rebuilds the very same dataset.
 *
 * The algorithm is the classic 64 bits linear congruential generator whose
 * constants are the ones published by Donald Knuth.
 * ---------------------------------------------------------------------------
 */

/**
 * An immutable pseudo random number generator.
 *
 * @param seed internal state of the generator
 */
final case class Rng(private val seed: Long) {

  /**
   * Draws the next raw number of the sequence.
   *
   * @return the drawn number and the generator to use for the next draw
   */
  def nextLong: (Long, Rng) = {
    val next = seed * Rng.multiplier + Rng.increment
    (next, Rng(next))
  }

  /**
   * Draws a number between zero (included) and the given bound (excluded).
   *
   * @param bound strictly positive upper bound of the drawn number
   * @return the drawn number and the generator to use for the next draw
   */
  def nextInt(bound: Int): (Int, Rng) = {
    val (raw, next) = nextLong
    /* The unsigned shift keeps the value positive before the modulo. */
    (((raw >>> 16) % bound).toInt, next)
  }

  /**
   * Draws a number between zero (included) and one (excluded).
   *
   * @return the drawn number and the generator to use for the next draw
   */
  def nextDouble: (Double, Rng) = {
    val (raw, next) = nextLong
    ((raw >>> 11).toDouble / Rng.doubleUnit, next)
  }

  /**
   * Draws one element of a non empty sequence, with a uniform probability.
   *
   * @param values sequence to draw from, it must not be empty
   * @tparam A type of the elements of the sequence
   * @return the drawn element and the generator to use for the next draw
   */
  def pick[A](values: Vector[A]): (A, Rng) = {
    val (index, next) = nextInt(values.length)
    (values(index), next)
  }
}

/**
 * Companion object of [[Rng]], holding the constants of the algorithm.
 */
object Rng {

  /** Multiplier of the linear congruential generator. */
  private val multiplier: Long = 6364136223846793005L

  /** Increment of the linear congruential generator. */
  private val increment: Long = 1442695040888963407L

  /** Number of distinct values a fifty three bits mantissa can hold. */
  private val doubleUnit: Double = (1L << 53).toDouble
}
