package com.hit.fp.io

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.SaveMode

/*
 * ---------------------------------------------------------------------------
 * DataSink.scala
 *
 * Output layer of the pipeline. This object is the only place of the project
 * that writes the reports to the disk, which keeps every analytical job free
 * of any input/output concern.
 *
 * Each report is small (a few dozen lines at most), therefore the Dataset is
 * repartitioned into a single partition before being written, so that the
 * produced report is one readable CSV file instead of two hundred fragments.
 * ---------------------------------------------------------------------------
 */

/**
 * Writes the reports produced by the pipeline.
 */
object DataSink {

  /**
   * Writes a report as a single CSV file, with its header line.
   *
   * @param report report to write
   * @param path   directory the report is written to
   * @tparam A type of the records of the report
   */
  def writeCsv[A](report: Dataset[A], path: String): Unit =
    report
      .coalesce(1)
      .write
      .mode(SaveMode.Overwrite)
      .option("header", "true")
      .csv(path)
}
