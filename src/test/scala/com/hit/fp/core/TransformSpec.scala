package com.hit.fp.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.hit.fp.model.Premium
import com.hit.fp.model.Small

/*
 * ---------------------------------------------------------------------------
 * TransformSpec.scala
 *
 * Unit tests of the custom combinator. The three combinators are checked one
 * by one, and the laws they have to respect (associativity of the sequential
 * combinator, neutrality of the identity transformation) are checked too.
 * ---------------------------------------------------------------------------
 */

/**
 * Verifies the custom combinator of the project.
 */
class TransformSpec extends AnyFunSuite with Matchers {

  /** A transformation doubling the number it receives. */
  private val doubled: Transform[Int, Int] =
    Transform.lift((value: Int) => value * 2)

  /** A transformation rendering the number it receives. */
  private val rendered: Transform[Int, String] =
    Transform.lift((value: Int) => "value=" + value)

  test("a lifted function behaves like the function it was built from") {
    doubled.run(21) shouldBe 42
  }

  test("the sequential combinator applies the steps in order") {
    (doubled ~> rendered).run(4) shouldBe "value=8"
  }

  test("the sequential combinator is associative") {
    val plusOne = Transform.lift((value: Int) => value + 1)
    val left = (doubled ~> plusOne) ~> rendered
    val right = doubled ~> (plusOne ~> rendered)
    left.run(5) shouldBe right.run(5)
  }

  test("the identity transformation is neutral") {
    (Transform.identity[Int] ~> doubled).run(7) shouldBe doubled.run(7)
    (doubled ~> Transform.identity[Int]).run(7) shouldBe doubled.run(7)
  }

  test("the parallel combinator pairs the results of both steps") {
    (doubled zip rendered).run(3) shouldBe ((6, "value=3"))
  }

  test("the filtering combinator drops the refused results") {
    val onlyBig = doubled.filterK(value => value > 10)
    onlyBig.run(6) shouldBe Some(12)
    onlyBig.run(2) shouldBe None
  }

  test("a whole list of steps is combined by the chain constructor") {
    val steps = List.fill(3)(Transform.lift((value: Int) => value + 5))
    Transform.chain(steps).run(0) shouldBe 15
    Transform.chain(List.empty[Transform[Int, Int]]).run(9) shouldBe 9
  }

  test("the combinator classifies a revenue like the core does") {
    val classify =
      Transform.lift((revenue: Double) => PureAnalytics.classifyOrder(revenue))
    val labelOf = Transform.lift((size: com.hit.fp.model.OrderSize) =>
      size.label
    )
    (classify ~> labelOf).run(5.0) shouldBe Small.label
    (classify ~> labelOf).run(5000.0) shouldBe Premium.label
  }
}
