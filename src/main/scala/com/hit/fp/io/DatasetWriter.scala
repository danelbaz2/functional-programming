package com.hit.fp.io

import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

import com.hit.fp.core.DataGeneration
import com.hit.fp.core.Rng

/*
 * ---------------------------------------------------------------------------
 * DatasetWriter.scala
 *
 * Input/output layer of the dataset generation. This object is deliberately
 * impure: it creates directories and it writes files. Everything it writes
 * was however computed by the pure functions of DataGeneration, so the two
 * responsibilities stay in two different objects, in two different packages.
 *
 * The generation is skipped when the files already exist, so that running the
 * pipeline twice does not rebuild fifty thousand lines for nothing.
 * ---------------------------------------------------------------------------
 */

/**
 * Writes the generated dataset to the disk.
 */
object DatasetWriter {

  /**
   * Generates the source files of the pipeline unless they already exist.
   *
   * @param config configuration holding the paths and the wanted sizes
   * @return true when the files were generated, false when they existed
   */
  def generateIfMissing(config: PipelineConfig): Boolean = {
    val transactionsFile = new File(config.transactionsPath)
    val productsFile = new File(config.productsPath)
    if (transactionsFile.exists() && productsFile.exists()) {
      false
    } else {
      generate(config)
      true
    }
  }

  /**
   * Generates the source files of the pipeline, overwriting existing ones.
   *
   * @param config configuration holding the paths and the wanted sizes
   */
  def generate(config: PipelineConfig): Unit = {
    val (products, afterProducts) =
      DataGeneration.generateProducts(config.productCount, Rng(config.seed))
    val (transactions, _) = DataGeneration.generateTransactions(
      config.transactionCount,
      products,
      afterProducts
    )
    writeLines(config.productsPath, DataGeneration.productLines(products))
    writeLines(
      config.transactionsPath,
      DataGeneration.transactionLines(transactions)
    )
  }

  /**
   * Writes a whole list of lines to a text file, creating its directory.
   *
   * @param path  path of the written file
   * @param lines lines to write, in the order they have to appear
   */
  private def writeLines(path: String, lines: List[String]): Unit = {
    val file = new File(path)
    Option(file.getParentFile).foreach(parent => parent.mkdirs())
    val writer = new PrintWriter(file, StandardCharsets.UTF_8.name())
    try {
      lines.foreach(line => writer.println(line))
    } finally {
      writer.close()
    }
  }
}
