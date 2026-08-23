package com.hit.fp.core

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.hit.fp.model.Product
import com.hit.fp.model.Transaction

/*
 * ---------------------------------------------------------------------------
 * CsvParsing.scala
 *
 * Pure parsing layer of the pipeline. Not a single function of this object
 * reads a file, writes a file or prints anything: they all receive a raw text
 * line and return either a domain value or a description of the failure.
 *
 * This is where the separation between the pure logic and the input/output
 * operations starts. The Spark jobs read the lines (input/output) and hand
 * them over to these pure functions, which is exactly why the parsing rules
 * can be unit tested without starting a Spark session at all.
 *
 * The small validation helpers below are curried: their first parameter list
 * captures the raw line, so that partially applying them yields a validator
 * dedicated to that line. That validator is then reused for every field.
 * ---------------------------------------------------------------------------
 */

/**
 * Pure functions that turn raw CSV lines into immutable domain values.
 */
object CsvParsing {

  /** Number of comma separated fields of a transaction line. */
  private val transactionFieldCount: Int = 7

  /** Number of comma separated fields of a product line. */
  private val productFieldCount: Int = 4

  /** First field of the header line of the transactions file. */
  private val transactionHeaderStart: String = "orderId"

  /** First field of the header line of the products file. */
  private val productHeaderStart: String = "productId"

  /**
   * Tells whether a raw line is the header line of one of the source files.
   *
   * @param line raw line to inspect
   * @return true when the line is a header line and has to be skipped
   */
  def isHeader(line: String): Boolean =
    line.startsWith(transactionHeaderStart) ||
      line.startsWith(productHeaderStart)

  /**
   * Parses a raw line of the transactions file.
   *
   * The body is a for comprehension over Either: as soon as one of the steps
   * produces a Left, the whole comprehension short circuits and returns that
   * Left, which is functional error handling without any exception.
   *
   * @param line raw comma separated line
   * @return the parsed transaction, or the error that prevented parsing it
   */
  def parseTransaction(line: String): Either[ParseError, Transaction] = {
    val fields = line.split(",", -1).map(field => field.trim)
    /* A validator and a number reader dedicated to this very line. */
    val text = nonEmptyField(line) _
    val positive = positiveNumber(line) _
    if (fields.length != transactionFieldCount) {
      Left(WrongFieldCount(line, transactionFieldCount, fields.length))
    } else {
      for {
        orderId <- text("orderId", fields(0)).right
        customerId <- text("customerId", fields(1)).right
        productId <- text("productId", fields(2)).right
        quantity <- positive("quantity", fields(3)).right
        unitPrice <- positive("unitPrice", fields(4)).right
        country <- text("country", fields(5)).right
        orderDate <- text("orderDate", fields(6)).right
      } yield Transaction(
        orderId = orderId,
        customerId = customerId,
        productId = productId,
        quantity = quantity.toInt,
        unitPrice = unitPrice,
        country = country,
        orderDate = orderDate
      )
    }
  }

  /**
   * Parses a raw line of the products file.
   *
   * @param line raw comma separated line
   * @return the parsed product, or the error that prevented parsing it
   */
  def parseProduct(line: String): Either[ParseError, Product] = {
    val fields = line.split(",", -1).map(field => field.trim)
    val text = nonEmptyField(line) _
    val positive = positiveNumber(line) _
    if (fields.length != productFieldCount) {
      Left(WrongFieldCount(line, productFieldCount, fields.length))
    } else {
      for {
        productId <- text("productId", fields(0)).right
        productName <- text("productName", fields(1)).right
        category <- text("category", fields(2)).right
        basePrice <- positive("basePrice", fields(3)).right
      } yield Product(
        productId = productId,
        productName = productName,
        category = category,
        basePrice = basePrice
      )
    }
  }

  /**
   * Curried validator of a mandatory textual field.
   *
   * @param line  raw line the field was taken from
   * @param field name of the validated field
   * @param value text found in the field
   * @return the trimmed text, or the error describing why it is illegal
   */
  private def nonEmptyField(
      line: String
  )(field: String, value: String): Either[ParseError, String] =
    if (value.isEmpty) Left(MissingField(line, field)) else Right(value)

  /**
   * Curried reader of a strictly positive numeric field.
   *
   * The result of Try is inspected with a pattern match, which turns the
   * exception thrown by toDouble into an ordinary immutable error value.
   *
   * @param line  raw line the field was taken from
   * @param field name of the validated field
   * @param value text found in the field
   * @return the parsed number, or the error describing why it is illegal
   */
  private def positiveNumber(
      line: String
  )(field: String, value: String): Either[ParseError, Double] =
    Try(value.toDouble) match {
      case Failure(_) => Left(NotANumber(line, field, value))
      case Success(number) if number <= 0.0 =>
        Left(OutOfRange(line, field, value))
      case Success(number) => Right(number)
    }
}
