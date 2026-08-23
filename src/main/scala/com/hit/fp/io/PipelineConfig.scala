package com.hit.fp.io

/*
 * ---------------------------------------------------------------------------
 * PipelineConfig.scala
 *
 * Immutable configuration of one run of the pipeline. Every path and every
 * threshold used by the jobs is gathered here, so that no job ever reads a
 * hard coded path and so that a test can run the whole pipeline on a
 * temporary directory simply by building another configuration value.
 *
 * The thresholds of the configuration are also what the Spark jobs capture in
 * their closures: the configuration lives on the driver, the closure that
 * uses one of its fields is serialized and shipped to the executors.
 * ---------------------------------------------------------------------------
 */

/**
 * Configuration of one run of the pipeline.
 *
 * @param dataDirectory      directory holding the generated source files
 * @param outputDirectory    directory the reports are written to
 * @param transactionCount   number of transactions to generate
 * @param productCount       number of products to generate
 * @param seed               seed of the pseudo random generator
 * @param minimalRevenue     revenue under which a line is not analysed
 * @param topCustomerCount   number of customers kept in the customer report
 * @param paretoThreshold    share of the revenue used by the Pareto finding
 */
final case class PipelineConfig(
    dataDirectory: String,
    outputDirectory: String,
    transactionCount: Int,
    productCount: Int,
    seed: Long,
    minimalRevenue: Double,
    topCustomerCount: Int,
    paretoThreshold: Double
) {

  /** Path of the generated transactions file. */
  def transactionsPath: String = dataDirectory + "/transactions.csv"

  /** Path of the generated products file. */
  def productsPath: String = dataDirectory + "/products.csv"

  /** Path of the directory holding the revenue by category report. */
  def categoryReportPath: String = outputDirectory + "/revenue_by_category"

  /** Path of the directory holding the revenue by country report. */
  def countryReportPath: String = outputDirectory + "/revenue_by_country"

  /** Path of the directory holding the best customers report. */
  def customerReportPath: String = outputDirectory + "/top_customers"

  /** Path of the directory holding the order size report. */
  def orderSizeReportPath: String = outputDirectory + "/order_sizes"

  /** Path of the directory holding the Pareto report. */
  def paretoReportPath: String = outputDirectory + "/pareto_categories"

  /** Path of the directory holding the rejected source lines. */
  def rejectedReportPath: String = outputDirectory + "/rejected_lines"
}

/**
 * Companion object of [[PipelineConfig]], holding the default configuration.
 */
object PipelineConfig {

  /** Configuration used when the pipeline is started without arguments. */
  val default: PipelineConfig = PipelineConfig(
    dataDirectory = "data",
    outputDirectory = "output",
    transactionCount = 15000,
    productCount = 200,
    seed = 20260816L,
    minimalRevenue = 20.0,
    topCustomerCount = 20,
    paretoThreshold = 80.0
  )
}
