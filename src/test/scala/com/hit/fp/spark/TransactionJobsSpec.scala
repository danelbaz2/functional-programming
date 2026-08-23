package com.hit.fp.spark

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.hit.fp.model.EnrichedTransaction
import com.hit.fp.model.Product
import com.hit.fp.model.Transaction

/*
 * ---------------------------------------------------------------------------
 * TransactionJobsSpec.scala
 *
 * Unit tests of the distributed jobs. A single local Spark session is opened
 * for the whole suite and closed at its end, and every job is checked on a
 * tiny dataset whose expected results were computed by hand.
 * ---------------------------------------------------------------------------
 */

/**
 * Verifies the Spark jobs of the pipeline on a local Spark session.
 */
class TransactionJobsSpec
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll {

  /** Local Spark session shared by every test of this suite. */
  private var spark: SparkSession = _

  /** Opens the local Spark session before the first test. */
  override def beforeAll(): Unit =
    spark = SparkSessionProvider.session("TransactionJobsSpec")

  /** Closes the local Spark session after the last test. */
  override def afterAll(): Unit =
    if (spark != null) spark.stop()

  /**
   * Builds the three products of the tiny test catalog.
   *
   * @return the Dataset holding the test catalog
   */
  private def products: Dataset[Product] = {
    /* A stable value is needed to import the encoders of the session. */
    val session = spark
    import session.implicits._
    Seq(
      Product("P1", "Smart Lamp 1", "Garden", 10.0),
      Product("P2", "Urban Jacket 2", "Clothing", 100.0),
      Product("P3", "Deluxe Speaker 3", "Electronics", 500.0)
    ).toDS()
  }

  /**
   * Builds the four transactions of the tiny test dataset.
   *
   * @return the Dataset holding the test transactions
   */
  private def transactions: Dataset[Transaction] = {
    val session = spark
    import session.implicits._
    Seq(
      Transaction("T1", "C1", "P1", 1, 10.0, "Israel", "2024-01-01"),
      Transaction("T2", "C1", "P2", 2, 100.0, "Israel", "2024-01-02"),
      Transaction("T3", "C2", "P3", 4, 500.0, "Japan", "2024-01-03"),
      Transaction("T4", "C2", "P1", 3, 10.0, "Japan", "2024-01-04")
    ).toDS()
  }

  /**
   * Builds the enriched Dataset every aggregation test starts from.
   *
   * @return the enriched test transactions
   */
  private def enriched: Dataset[EnrichedTransaction] =
    TransactionJobs.enrich(transactions, products)

  test("the join enriches every transaction with its category") {
    val joined = enriched.collect().toList.sortBy(line => line.orderId)
    joined.length shouldBe 4
    joined.map(line => line.category) shouldBe
      List("Garden", "Clothing", "Electronics", "Garden")
    joined.map(line => line.revenue) shouldBe
      List(10.0, 200.0, 2000.0, 30.0)
  }

  test("the filter drops the transactions that are too small") {
    val kept = TransactionJobs.keepSignificant(enriched, 50.0)
    kept.collect().map(line => line.orderId).sorted shouldBe
      Array("T2", "T3")
  }

  test("the revenue is aggregated by category") {
    val byCategory = TransactionJobs
      .revenueByCategory(enriched)
      .collect()
      .map(entry => (entry.category, entry.revenue))
      .toMap
    byCategory("Garden") shouldBe 40.0 +- 0.0001
    byCategory("Clothing") shouldBe 200.0 +- 0.0001
    byCategory("Electronics") shouldBe 2000.0 +- 0.0001
  }

  test("the revenue is aggregated by country with the RDD API") {
    val byCountry = TransactionJobs
      .revenueByCountry(enriched)
      .collect()
      .map(entry => (entry.country, entry.revenue))
      .toMap
    byCountry("Israel") shouldBe 210.0 +- 0.0001
    byCountry("Japan") shouldBe 2030.0 +- 0.0001
  }

  test("the best customers are ranked by the amount they spent") {
    val best = TransactionJobs.topCustomers(enriched, 1).collect().toList
    best.length shouldBe 1
    best.head.customerId shouldBe "C2"
    best.head.totalSpent shouldBe 2030.0 +- 0.0001
    best.head.orders shouldBe 2L
  }

  test("the order sizes are counted by classification") {
    val bySize = TransactionJobs
      .orderSizeDistribution(enriched)
      .collect()
      .map(entry => (entry.size, entry.count))
      .toMap
    bySize.getOrElse("SMALL", 0L) shouldBe 2L
    bySize.getOrElse("LARGE", 0L) shouldBe 1L
    bySize.getOrElse("PREMIUM", 0L) shouldBe 1L
  }

  test("the Pareto analysis is built from the aggregated categories") {
    val pareto = TransactionJobs
      .paretoByCategory(TransactionJobs.revenueByCategory(enriched))
      .collect()
      .toList
    val richest = pareto.sortBy(line => -line.revenue).head
    val poorest = pareto.sortBy(line => -line.revenue).last
    richest.category shouldBe "Electronics"
    poorest.cumulativeShare shouldBe 100.0 +- 0.01
  }
}
