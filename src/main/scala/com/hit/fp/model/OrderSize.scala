package com.hit.fp.model

/*
 * ---------------------------------------------------------------------------
 * OrderSize.scala
 *
 * Algebraic data type that classifies an order according to its revenue. The
 * trait is sealed, therefore the compiler knows the complete list of its
 * implementations and is able to warn about a non exhaustive pattern match.
 *
 * This type is the one used to demonstrate the "pattern matching with case
 * classes" advanced functional programming technique: the classification
 * function of the pure core matches on the revenue and produces one of the
 * case objects below, and the reporting function matches back on the produced
 * value in order to render it.
 * ---------------------------------------------------------------------------
 */

/**
 * Classification of an order according to the revenue it generated.
 */
sealed trait OrderSize {

  /** Human readable label of the classification, used by the reports. */
  def label: String
}

/**
 * Companion object holding the ordered list of the possible classifications.
 */
object OrderSize {

  /** Every classification, from the cheapest one to the most expensive one. */
  val all: List[OrderSize] = List(Small, Medium, Large, Premium)
}

/** An order whose revenue is lower than fifty dollars. */
case object Small extends OrderSize {

  /** @inheritdoc */
  override def label: String = "SMALL"
}

/** An order whose revenue is between fifty and two hundred dollars. */
case object Medium extends OrderSize {

  /** @inheritdoc */
  override def label: String = "MEDIUM"
}

/** An order whose revenue is between two hundred and one thousand dollars. */
case object Large extends OrderSize {

  /** @inheritdoc */
  override def label: String = "LARGE"
}

/** An order whose revenue is at least one thousand dollars. */
case object Premium extends OrderSize {

  /** @inheritdoc */
  override def label: String = "PREMIUM"
}
