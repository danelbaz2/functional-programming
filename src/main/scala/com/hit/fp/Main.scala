package com.hit.fp

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

import com.hit.fp.core.PureAnalytics
import com.hit.fp.core.ReportFormatting
import com.hit.fp.io.ConsoleReport
import com.hit.fp.io.DataSink
import com.hit.fp.io.DataSource
import com.hit.fp.io.DatasetWriter
import com.hit.fp.io.PipelineConfig
import com.hit.fp.spark.SparkSessionProvider
import com.hit.fp.spark.TransactionJobs

/*
 * ---------------------------------------------------------------------------
 * Main.scala
 *
 * Entry point of the project. This object is the impure shell of the whole
 * pipeline: it generates the source files if they are missing, it opens the
 * Spark session, it asks the jobs to build the reports, it writes them and it
 * prints the findings. Every rule it applies comes from the pure core.
 *
 * The order of the run is the following:
 *   1. generate the dataset            (com.hit.fp.io.DatasetWriter)
 *   2. read the two source files       (com.hit.fp.io.DataSource)
 *   3. join, filter and aggregate      (com.hit.fp.spark.TransactionJobs)
 *   4. write the six reports           (com.hit.fp.io.DataSink)
 *   5. print the findings              (com.hit.fp.io.ConsoleReport)
 * ---------------------------------------------------------------------------
 */

/**
 * Runs the whole functional data processing pipeline.
 */
object Main {

  /** Name of the application shown by the Spark user interface. */
  private val applicationName: String = "FunctionalDataPipeline"

  /**
   * Starts the pipeline.
   *
   * @param args command line arguments, they are not used
   */
  def main(args: Array[String]): Unit = {
    val config = PipelineConfig.default
    val generated = DatasetWriter.generateIfMissing(config)
    ConsoleReport.line(
      if (generated) "Dataset generated in " + config.dataDirectory
      else "Dataset already present in " + config.dataDirectory
    )
    val spark = SparkSessionProvider.session(applicationName)
    try {
      run(spark, config)
    } finally {
      spark.stop()
    }
  }

  /**
   * Runs every step of the pipeline on an open Spark session.
   *
   * @param spark  session used by every job
   * @param config configuration of the run
   */
  def run(spark: SparkSession, config: PipelineConfig): Unit = {
    /* Step 2 : read the two external sources. */
    val transactions = DataSource.loadTransactions(
      spark,
      config.transactionsPath
    )
    val products = DataSource.loadProducts(spark, config.productsPath)
    val rejected =
      DataSource.loadRejectedTransactions(spark, config.transactionsPath)

    /* Step 3 : join, filter and aggregate. */
    val enriched = TransactionJobs
      .enrich(transactions, products)
      .persist(StorageLevel.MEMORY_AND_DISK)
    val significant =
      TransactionJobs.keepSignificant(enriched, config.minimalRevenue)
    val categories = TransactionJobs.revenueByCategory(significant)
    val countries = TransactionJobs.revenueByCountry(significant)
    val customers =
      TransactionJobs.topCustomers(significant, config.topCustomerCount)
    val sizes = TransactionJobs.orderSizeDistribution(enriched)
    val pareto = TransactionJobs.paretoByCategory(categories)

    /* Step 4 : write every report to the output directory. */
    DataSink.writeCsv(categories, config.categoryReportPath)
    DataSink.writeCsv(countries, config.countryReportPath)
    DataSink.writeCsv(customers, config.customerReportPath)
    DataSink.writeCsv(sizes, config.orderSizeReportPath)
    DataSink.writeCsv(pareto, config.paretoReportPath)
    DataSink.writeCsv(rejected, config.rejectedReportPath)

    /* Step 5 : print the findings, rendered by the pure core. */
    val categoryList = categories.collect().toList
    val paretoList = pareto.collect().toList
    ConsoleReport.section(
      "SOURCE FILE QUALITY",
      ReportFormatting.qualityLines(transactions.count(), rejected.count())
    )
    ConsoleReport.section(
      "REVENUE BY CATEGORY",
      ReportFormatting.categoryLines(categoryList)
    )
    ConsoleReport.section(
      "REVENUE BY COUNTRY",
      ReportFormatting.countryLines(countries.collect().toList)
    )
    ConsoleReport.section(
      "TOP CUSTOMERS",
      ReportFormatting.customerLines(customers.collect().toList)
    )
    ConsoleReport.section(
      "ORDER SIZE DISTRIBUTION",
      ReportFormatting.orderSizeLines(sizes.collect().toList)
    )
    ConsoleReport.section(
      "PARETO ANALYSIS OF THE CATEGORIES",
      ReportFormatting.paretoLines(paretoList, config.paretoThreshold)
    )
    ConsoleReport.section(
      "TOTAL",
      List(
        "total revenue of the analysed lines : %.2f".format(
          PureAnalytics.roundMoney(
            categoryList.foldLeft(0.0)((sum, entry) => sum + entry.revenue)
          )
        ),
        "reports written to " + config.outputDirectory
      )
    )
    enriched.unpersist()
  }
}
