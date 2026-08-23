package com.hit.fp.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/*
 * ---------------------------------------------------------------------------
 * CsvParsingSpec.scala
 *
 * Unit tests of the pure parsing layer. Not one of these tests starts a Spark
 * session or reads a file, which is exactly the benefit of having kept the
 * parsing rules in pure functions.
 * ---------------------------------------------------------------------------
 */

/**
 * Verifies that the pure parser accepts legal lines and describes illegal
 * ones with the right error value.
 */
class CsvParsingSpec extends AnyFunSuite with Matchers {

  /** A perfectly legal line of the transactions file. */
  private val legalLine: String =
    "T0000001,C00042,P0007,3,19.90,Israel,2024-03-11"

  test("a legal transaction line is parsed into a transaction") {
    val parsed = CsvParsing.parseTransaction(legalLine)
    parsed.isRight shouldBe true
    val transaction = parsed.right.get
    transaction.orderId shouldBe "T0000001"
    transaction.customerId shouldBe "C00042"
    transaction.productId shouldBe "P0007"
    transaction.quantity shouldBe 3
    transaction.unitPrice shouldBe 19.90 +- 0.0001
    transaction.country shouldBe "Israel"
    transaction.orderDate shouldBe "2024-03-11"
  }

  test("a line with a missing field is rejected as a wrong field count") {
    val parsed = CsvParsing.parseTransaction("T1,C1,P1,3,19.90,Israel")
    parsed.left.get shouldBe a[WrongFieldCount]
  }

  test("a price that is not a number is rejected as not a number") {
    val broken = "T1,C1,P1,3,N/A,Israel,2024-03-11"
    val parsed = CsvParsing.parseTransaction(broken)
    parsed.left.get shouldBe a[NotANumber]
  }

  test("a negative quantity is rejected as out of range") {
    val broken = "T1,C1,P1,-2,19.90,Israel,2024-03-11"
    val parsed = CsvParsing.parseTransaction(broken)
    parsed.left.get shouldBe a[OutOfRange]
  }

  test("an empty mandatory field is rejected as a missing field") {
    val broken = "T1,,P1,3,19.90,Israel,2024-03-11"
    val parsed = CsvParsing.parseTransaction(broken)
    parsed.left.get shouldBe a[MissingField]
  }

  test("a legal product line is parsed into a product") {
    val parsed = CsvParsing.parseProduct("P0007,Smart Lamp 7,Garden,49.5")
    parsed.isRight shouldBe true
    parsed.right.get.category shouldBe "Garden"
    parsed.right.get.basePrice shouldBe 49.5 +- 0.0001
  }

  test("the header lines of both source files are recognised") {
    CsvParsing.isHeader(DataGeneration.transactionHeader) shouldBe true
    CsvParsing.isHeader(DataGeneration.productHeader) shouldBe true
    CsvParsing.isHeader(legalLine) shouldBe false
  }

  test("parsing the same line twice always returns the same result") {
    CsvParsing.parseTransaction(legalLine) shouldBe
      CsvParsing.parseTransaction(legalLine)
  }
}
