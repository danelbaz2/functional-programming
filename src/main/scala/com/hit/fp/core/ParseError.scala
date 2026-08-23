package com.hit.fp.core

/*
 * ---------------------------------------------------------------------------
 * ParseError.scala
 *
 * ADVANCED FUNCTIONAL PROGRAMMING TECHNIQUE 2 : FUNCTIONAL ERROR HANDLING.
 *
 * The pipeline never throws an exception when it meets a corrupted line of
 * the source file. Instead, every parsing function returns an Either whose
 * left side is one of the errors described here, and whose right side is the
 * successfully parsed value.
 *
 * Errors are therefore ordinary immutable values: they can be counted,
 * filtered, grouped and written to a file exactly like any other record of
 * the pipeline, and the type system forces every caller to deal with them.
 *
 * The trait is sealed so the compiler checks the exhaustiveness of the
 * pattern matches performed on it, and Serializable because these values
 * travel between the Spark driver and the Spark executors.
 * ---------------------------------------------------------------------------
 */

/**
 * A recoverable error met while a raw line of the source file was parsed.
 */
sealed trait ParseError extends Serializable {

  /** Raw line that could not be parsed. */
  def line: String

  /** Human readable explanation of the failure. */
  def reason: String
}

/**
 * The line does not hold the expected number of comma separated fields.
 *
 * @param line     raw line that could not be parsed
 * @param expected number of fields the format requires
 * @param found    number of fields the line actually holds
 */
final case class WrongFieldCount(line: String, expected: Int, found: Int)
    extends ParseError {

  /** @inheritdoc */
  override def reason: String =
    "expected " + expected + " fields but found " + found
}

/**
 * A field that has to hold a number holds something else.
 *
 * @param line  raw line that could not be parsed
 * @param field name of the offending field
 * @param value text that was found instead of a number
 */
final case class NotANumber(line: String, field: String, value: String)
    extends ParseError {

  /** @inheritdoc */
  override def reason: String =
    "field " + field + " is not a number: '" + value + "'"
}

/**
 * A field holds a number, but the number is outside of its legal range.
 *
 * @param line  raw line that could not be parsed
 * @param field name of the offending field
 * @param value text of the illegal value
 */
final case class OutOfRange(line: String, field: String, value: String)
    extends ParseError {

  /** @inheritdoc */
  override def reason: String =
    "field " + field + " is out of range: '" + value + "'"
}

/**
 * A mandatory textual field is empty.
 *
 * @param line  raw line that could not be parsed
 * @param field name of the offending field
 */
final case class MissingField(line: String, field: String) extends ParseError {

  /** @inheritdoc */
  override def reason: String = "field " + field + " is empty"
}
