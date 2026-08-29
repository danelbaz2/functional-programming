package com.hit.fp.io

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.hit.fp.spark.SparkSessionProvider

/*
 * ---------------------------------------------------------------------------
 * DataSourceSpec.scala
 *
 * Unit tests of the input layer. A single local Spark session is opened for
 * the whole suite and closed at its end.
 *
 * The suite checks the contract the functional error handling of the project
 * rests on: a corrupted line never interrupts a run, it is simply routed to
 * the rejected report instead of the accepted one. Nothing is lost and
 * nothing is counted twice, so the accepted lines and the rejected lines
 * always partition the source file.
 *
 * The second half of the suite pins that contract on the versioned dataset
 * of data/, whose exact figures are quoted by the report of the project.
 * ---------------------------------------------------------------------------
 */

/**
 * Verifies that the input layer splits a source file into accepted
 * transactions and rejected lines without losing any of them.
 */
class DataSourceSpec
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll {

  /** Local Spark session shared by every test of this suite. */
  private var spark: SparkSession = _

  /** Directory holding the temporary source file of the first test. */
  private var temporaryDirectory: Path = _

  /** Number of transactions the versioned source file holds. */
  private val versionedLineCount: Long = 15000L

  /** Number of lines of that file the parser accepts. */
  private val versionedAcceptedCount: Long = 14980L

  /** Number of lines of that file whose price was damaged. */
  private val versionedNotANumberCount: Int = 15

  /** Number of lines of that file that lost one of their fields. */
  private val versionedWrongFieldCount: Int = 5

  /** Opens the local Spark session before the first test. */
  override def beforeAll(): Unit = {
    spark = SparkSessionProvider.session("DataSourceSpec")
    temporaryDirectory = Files.createTempDirectory("data-source-spec")
  }

  /** Closes the session and removes the temporary files after the last test. */
  override def afterAll(): Unit = {
    if (spark != null) spark.stop()
    if (temporaryDirectory != null) {
      Files
        .list(temporaryDirectory)
        .forEach(file => Files.deleteIfExists(file))
      Files.deleteIfExists(temporaryDirectory)
    }
  }

  /**
   * Writes the given lines into a temporary file readable by Spark.
   *
   * @param name  name of the written file
   * @param lines lines to write, the header line included
   * @return the path of the written file, in the form Spark expects
   */
  private def temporaryFile(name: String, lines: List[String]): String = {
    val file = temporaryDirectory.resolve(name)
    val content = lines.mkString("\n") + "\n"
    Files.write(file, content.getBytes(StandardCharsets.UTF_8))
    file.toString.replace('\\', '/')
  }

  /** Path of the versioned transactions file analysed by the pipeline. */
  private def versionedFile: String = PipelineConfig.default.transactionsPath

  test("the accepted and the rejected lines partition a source file") {
    val path = temporaryFile(
      "mixed.csv",
      List(
        "orderId,customerId,productId,quantity,unitPrice,country,orderDate",
        "T1,C1,P1,1,10.00,Israel,2024-01-01",
        "T2,C1,P2,2,20.00,Japan,2024-01-02",
        "T3,C2,P3,3,30.00,France,2024-01-03",
        /* The price of this line is not a number. */
        "T4,C2,P1,1,N/A,Israel,2024-01-04",
        /* This line lost its last field. */
        "T5,C3,P2,2,20.00,Japan"
      )
    )
    val accepted = DataSource.loadTransactions(spark, path).count()
    val rejected = DataSource.loadRejectedTransactions(spark, path).count()
    accepted shouldBe 3L
    rejected shouldBe 2L
    accepted + rejected shouldBe 5L
  }

  test("a rejected line is dropped from the accepted transactions") {
    val path = temporaryFile(
      "broken.csv",
      List(
        "orderId,customerId,productId,quantity,unitPrice,country,orderDate",
        "T1,C1,P1,1,10.00,Israel,2024-01-01",
        "T2,C1,P2,2,N/A,Japan,2024-01-02"
      )
    )
    val accepted = DataSource.loadTransactions(spark, path).collect().toList
    accepted.map(transaction => transaction.orderId) shouldBe List("T1")
    val rejected =
      DataSource.loadRejectedTransactions(spark, path).collect().toList
    rejected.map(line => line.line) shouldBe
      List("T2,C1,P2,2,N/A,Japan,2024-01-02")
  }

  test("the versioned dataset yields exactly 14980 accepted transactions") {
    DataSource
      .loadTransactions(spark, versionedFile)
      .count() shouldBe versionedAcceptedCount
  }

  test("the versioned dataset yields exactly twenty rejected lines") {
    val rejected =
      DataSource.loadRejectedTransactions(spark, versionedFile).collect().toList
    rejected.length shouldBe 20
    rejected.count(line => line.reason.contains("is not a number")) shouldBe
      versionedNotANumberCount
    rejected.count(line => line.reason.contains("expected 7 fields")) shouldBe
      versionedWrongFieldCount
  }

  test("the versioned dataset loses none of its lines") {
    val accepted = DataSource.loadTransactions(spark, versionedFile).count()
    val rejected =
      DataSource.loadRejectedTransactions(spark, versionedFile).count()
    accepted + rejected shouldBe versionedLineCount
  }
}
