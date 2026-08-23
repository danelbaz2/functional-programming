package com.hit.fp.io

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.SparkSession

import com.hit.fp.core.CsvParsing
import com.hit.fp.model.Product
import com.hit.fp.model.RejectedLine
import com.hit.fp.model.Transaction

/*
 * ---------------------------------------------------------------------------
 * DataSource.scala
 *
 * Input layer of the pipeline. This object is the only place of the project
 * that reads the source files, and it does nothing else: the moment a raw
 * line is available, it is handed over to the pure functions of CsvParsing.
 *
 * Every loading function is written with the same three Spark steps:
 *   textFile  reads the external source as a distributed Dataset of lines,
 *   filter    drops the header line and the empty lines,
 *   flatMap   keeps the values the pure parser accepted, or the errors it
 *             produced, and drops the rest.
 *
 * Because the parser returns an Either instead of throwing, a corrupted line
 * never stops the run: it simply flows into the rejected report.
 * ---------------------------------------------------------------------------
 */

/**
 * Reads the source files of the pipeline into typed Datasets.
 */
object DataSource {

  /**
   * Reads a text file as a Dataset of its meaningful lines.
   *
   * @param spark session used to read the file
   * @param path  path of the read file
   * @return every line of the file but the header line and the empty ones
   */
  def readLines(spark: SparkSession, path: String): Dataset[String] = {
    import spark.implicits._
    spark.read
      .textFile(path)
      .filter((line: String) => line.nonEmpty && !CsvParsing.isHeader(line))
  }

  /**
   * Loads the transactions that the pure parser accepted.
   *
   * @param spark session used to read the file
   * @param path  path of the transactions file
   * @return the successfully parsed transactions
   */
  def loadTransactions(
      spark: SparkSession,
      path: String
  ): Dataset[Transaction] = {
    import spark.implicits._
    readLines(spark, path)
      .flatMap((line: String) => CsvParsing.parseTransaction(line).toOption)
  }

  /**
   * Loads the products that the pure parser accepted.
   *
   * @param spark session used to read the file
   * @param path  path of the products file
   * @return the successfully parsed products
   */
  def loadProducts(spark: SparkSession, path: String): Dataset[Product] = {
    import spark.implicits._
    readLines(spark, path)
      .flatMap((line: String) => CsvParsing.parseProduct(line).toOption)
  }

  /**
   * Loads the lines of the transactions file that the parser refused.
   *
   * @param spark session used to read the file
   * @param path  path of the transactions file
   * @return one record per rejected line, with the reason of the rejection
   */
  def loadRejectedTransactions(
      spark: SparkSession,
      path: String
  ): Dataset[RejectedLine] = {
    import spark.implicits._
    readLines(spark, path).flatMap { (line: String) =>
      CsvParsing
        .parseTransaction(line)
        .left
        .toOption
        .map(error => RejectedLine(error.reason, error.line))
    }
  }
}
