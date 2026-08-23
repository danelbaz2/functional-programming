package com.hit.fp.io

/*
 * ---------------------------------------------------------------------------
 * ConsoleReport.scala
 *
 * Input/output layer of the reporting. This object holds the only calls to
 * println of the whole project: every line it prints was built by the pure
 * functions of com.hit.fp.core.ReportFormatting.
 * ---------------------------------------------------------------------------
 */

/**
 * Prints the findings of the pipeline on the standard output.
 */
object ConsoleReport {

  /**
   * Prints a whole section, its title first.
   *
   * @param title title of the section
   * @param lines lines rendered by the formatting functions of the core
   */
  def section(title: String, lines: List[String]): Unit = {
    com.hit.fp.core.ReportFormatting.title(title).foreach(println)
    lines.foreach(println)
  }

  /**
   * Prints a single line.
   *
   * @param line line to print
   */
  def line(line: String): Unit = println(line)
}
