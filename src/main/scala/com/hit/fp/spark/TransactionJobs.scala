package com.hit.fp.spark

import org.apache.spark.sql.Dataset

import com.hit.fp.core.PureAnalytics
import com.hit.fp.core.RevenueAccumulator
import com.hit.fp.core.Transform
import com.hit.fp.model.CategoryRevenue
import com.hit.fp.model.CountryRevenue
import com.hit.fp.model.CustomerSpending
import com.hit.fp.model.EnrichedTransaction
import com.hit.fp.model.OrderSize
import com.hit.fp.model.OrderSizeCount
import com.hit.fp.model.ParetoLine
import com.hit.fp.model.Product
import com.hit.fp.model.Transaction

/*
 * ---------------------------------------------------------------------------
 * TransactionJobs.scala
 *
 * The distributed part of the pipeline. Every function of this object takes
 * one or more Datasets and returns a new Dataset: no function mutates its
 * argument, and no function reads or writes a file.
 *
 * The jobs never implement business rules themselves. They only decide how
 * the pure functions of com.hit.fp.core.PureAnalytics are distributed over
 * the cluster, which is why the analytical logic can be tested without Spark.
 *
 * SPARK OPERATIONS USED HERE (six of them, four are required):
 *   joinWith     enrich, joins the transactions with the catalog
 *   map          enrich and orderSizeDistribution
 *   filter       keepSignificant
 *   groupByKey   revenueByCategory, spendingByCustomer, orderSizeDistribution
 *   reduceByKey  revenueByCountry, on the RDD API
 *   flatMap      used by the input layer, com.hit.fp.io.DataSource
 *
 * ADVANCED TECHNIQUE 5 : CLOSURES IN SPARK TRANSFORMATIONS. The predicate
 * built in keepSignificant captures the threshold that lives on the driver,
 * and Spark serializes that closure to every executor of the cluster.
 * ---------------------------------------------------------------------------
 */

/**
 * Distributed analytical jobs of the pipeline.
 */
object TransactionJobs {

  /**
   * Joins the transactions with the catalog and computes their revenue.
   *
   * @param transactions transactions read from the source file
   * @param products     catalog read from the source file
   * @return one enriched transaction per joined pair
   */
  def enrich(
      transactions: Dataset[Transaction],
      products: Dataset[Product]
  ): Dataset[EnrichedTransaction] = {
    val spark = transactions.sparkSession
    import spark.implicits._
    transactions
      .joinWith(
        products,
        transactions("productId") === products("productId")
      )
      .map {
        /* The pair produced by the typed join is destructured by a match. */
        case (transaction, product) =>
          PureAnalytics.enrich(transaction, product)
      }
  }

  /**
   * Keeps the transactions whose revenue is worth being analysed.
   *
   * @param enriched       enriched transactions to filter
   * @param minimalRevenue revenue a transaction has to reach to be kept
   * @return the transactions reaching the given revenue
   */
  def keepSignificant(
      enriched: Dataset[EnrichedTransaction],
      minimalRevenue: Double
  ): Dataset[EnrichedTransaction] = {
    /* Partially applying the curried predicate builds the closure. */
    val isSignificant: EnrichedTransaction => Boolean =
      PureAnalytics.atLeastRevenue(minimalRevenue)
    enriched.filter(isSignificant)
  }

  /**
   * Aggregates the revenue of every commercial category.
   *
   * @param enriched enriched transactions to aggregate
   * @return one record per category
   */
  def revenueByCategory(
      enriched: Dataset[EnrichedTransaction]
  ): Dataset[CategoryRevenue] = {
    val spark = enriched.sparkSession
    import spark.implicits._
    enriched
      .groupByKey((transaction: EnrichedTransaction) => transaction.category)
      .mapGroups { (category, rows) =>
        PureAnalytics.foldCategory(category, rows)
      }
  }

  /**
   * Aggregates the revenue of every destination country, with the RDD API.
   *
   * This is the only job of the pipeline written with the RDD API. It shows
   * the classic functional pair of Spark operations: a map producing a pair
   * whose key is the country, then a reduceByKey folding the values of every
   * key with an associative anonymous function.
   *
   * @param enriched enriched transactions to aggregate
   * @return one record per destination country
   */
  def revenueByCountry(
      enriched: Dataset[EnrichedTransaction]
  ): Dataset[CountryRevenue] = {
    val spark = enriched.sparkSession
    import spark.implicits._
    val reduced = enriched.rdd
      .map((transaction: EnrichedTransaction) =>
        (transaction.country, transaction.revenue)
      )
      .reduceByKey((left: Double, right: Double) => left + right)
      .map {
        case (country, revenue) =>
          CountryRevenue(country, PureAnalytics.roundMoney(revenue))
      }
    spark.createDataset(reduced)
  }

  /**
   * Computes the total amount spent by every customer.
   *
   * @param enriched enriched transactions to aggregate
   * @return one record per customer
   */
  def spendingByCustomer(
      enriched: Dataset[EnrichedTransaction]
  ): Dataset[CustomerSpending] = {
    val spark = enriched.sparkSession
    import spark.implicits._
    enriched
      .groupByKey((transaction: EnrichedTransaction) => transaction.customerId)
      .mapGroups { (customerId, rows) =>
        val folded =
          rows.foldLeft(RevenueAccumulator.empty)(PureAnalytics.accumulate)
        CustomerSpending(
          customerId = customerId,
          totalSpent = PureAnalytics.roundMoney(folded.revenue),
          orders = folded.orders
        )
      }
  }

  /**
   * Keeps the customers that spent the most.
   *
   * The selection itself is the pure and generic PureAnalytics.topBy
   * function, applied on the driver once the customers were aggregated by
   * the cluster.
   *
   * @param enriched enriched transactions to aggregate
   * @param count    number of customers to keep
   * @return the best customers, the biggest spender first
   */
  def topCustomers(
      enriched: Dataset[EnrichedTransaction],
      count: Int
  ): Dataset[CustomerSpending] = {
    val spark = enriched.sparkSession
    import spark.implicits._
    val everyCustomer = spendingByCustomer(enriched).collect().toList
    val best = PureAnalytics.topBy(everyCustomer, count) {
      (customer: CustomerSpending) => customer.totalSpent
    }
    spark.createDataset(best)
  }

  /**
   * Counts how many transactions fall in every order size classification.
   *
   * The classification is the pure pattern matching function of the core,
   * and it is applied here by a distributed map. The custom combinator of
   * com.hit.fp.core.Transform builds the function applied by that map, by
   * combining the two steps "read the revenue" and "classify the revenue".
   *
   * @param enriched enriched transactions to classify
   * @return one record per classification
   */
  def orderSizeDistribution(
      enriched: Dataset[EnrichedTransaction]
  ): Dataset[OrderSizeCount] = {
    val spark = enriched.sparkSession
    import spark.implicits._
    val readRevenue =
      Transform.lift((transaction: EnrichedTransaction) => transaction.revenue)
    val classify =
      Transform.lift((revenue: Double) => PureAnalytics.classifyOrder(revenue))
    val labelOf = Transform.lift((size: OrderSize) => size.label)
    /* The three steps are combined into one transformation by ~> . */
    val pipeline = readRevenue ~> classify ~> labelOf
    enriched
      .map((transaction: EnrichedTransaction) => pipeline.run(transaction))
      .groupByKey((label: String) => label)
      .count()
      .map {
        case (label, howMany) => OrderSizeCount(label, howMany)
      }
  }

  /**
   * Builds the Pareto analysis of the categories.
   *
   * @param categories aggregated revenue of every category
   * @return one Pareto line per category, richest category first
   */
  def paretoByCategory(
      categories: Dataset[CategoryRevenue]
  ): Dataset[ParetoLine] = {
    val spark = categories.sparkSession
    import spark.implicits._
    spark.createDataset(PureAnalytics.paretoOf(categories.collect().toList))
  }
}
