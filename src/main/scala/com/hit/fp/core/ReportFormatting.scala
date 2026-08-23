package com.hit.fp.core

import com.hit.fp.model.CategoryRevenue
import com.hit.fp.model.CountryRevenue
import com.hit.fp.model.CustomerSpending
import com.hit.fp.model.OrderSize
import com.hit.fp.model.OrderSizeCount
import com.hit.fp.model.ParetoLine

/*
 * ---------------------------------------------------------------------------
 * ReportFormatting.scala
 *
 * Pure rendering of the findings of the pipeline. Every function below takes
 * immutable values and returns the immutable list of the lines that describe
 * them; not a single one of them prints anything.
 *
 * Printing is an effect and it belongs to the input/output layer, in
 * com.hit.fp.io.ConsoleReport. Splitting the rendering from the printing is
 * what makes the layout of the final report unit testable.
 * ---------------------------------------------------------------------------
 */

/**
 * Pure functions rendering the findings of the pipeline as text lines.
 */
object ReportFormatting {

  /** Width of the separator line drawn under every title. */
  private val separatorWidth: Int = 62

  /**
   * Renders the title of a section, underlined by a separator line.
   *
   * @param text title of the section
   * @return the lines of the title
   */
  def title(text: String): List[String] =
    List("", text, "-" * separatorWidth)

  /**
   * Renders the revenue of every category, richest category first.
   *
   * @param categories aggregated revenue of every category
   * @return one line per category
   */
  def categoryLines(categories: List[CategoryRevenue]): List[String] =
    categories
      .sortBy(category => -category.revenue)
      .map { category =>
        "%-14s %14.2f  %8d orders  %8d units".format(
          category.category,
          category.revenue,
          category.orders,
          category.unitsSold
        )
      }

  /**
   * Renders the revenue of every country, richest country first.
   *
   * @param countries aggregated revenue of every country
   * @return one line per country
   */
  def countryLines(countries: List[CountryRevenue]): List[String] =
    countries
      .sortBy(country => -country.revenue)
      .map(country => "%-14s %14.2f".format(country.country, country.revenue))

  /**
   * Renders the best customers, the biggest spender first.
   *
   * @param customers best customers of the shop
   * @return one line per customer
   */
  def customerLines(customers: List[CustomerSpending]): List[String] =
    customers
      .sortBy(customer => -customer.totalSpent)
      .map { customer =>
        "%-10s %14.2f  %6d orders".format(
          customer.customerId,
          customer.totalSpent,
          customer.orders
        )
      }

  /**
   * Renders the distribution of the order sizes.
   *
   * The description of every classification is produced by the exhaustive
   * pattern match of PureAnalytics.describeSize.
   *
   * @param counts number of transactions of every classification
   * @return one line per classification
   */
  def orderSizeLines(counts: List[OrderSizeCount]): List[String] = {
    val byLabel = counts.map(entry => (entry.size, entry.count)).toMap
    OrderSize.all.map { size =>
      "%-8s %10d lines  (%s)".format(
        size.label,
        byLabel.getOrElse(size.label, 0L),
        PureAnalytics.describeSize(size)
      )
    }
  }

  /**
   * Renders the Pareto analysis and the finding it leads to.
   *
   * @param pareto    Pareto lines produced by PureAnalytics.paretoOf
   * @param threshold share of the revenue to reach, in percent
   * @return one line per category, followed by the finding
   */
  def paretoLines(pareto: List[ParetoLine], threshold: Double): List[String] = {
    val rows = pareto.map { line =>
      "%-14s %14.2f  %6.2f %% cumulated".format(
        line.category,
        line.revenue,
        line.cumulativeShare
      )
    }
    val needed = PureAnalytics.categoriesUpTo(pareto, threshold)
    val finding = "FINDING: %d categories out of %d generate %.0f %% of it."
      .format(needed, pareto.length, threshold)
    rows ++ List("", finding)
  }

  /**
   * Renders the health of the source file that was read.
   *
   * @param accepted number of lines the parser accepted
   * @param rejected number of lines the parser refused
   * @return the lines describing the quality of the source file
   */
  def qualityLines(accepted: Long, rejected: Long): List[String] = {
    val total = accepted + rejected
    val share = if (total <= 0L) 0.0 else rejected.toDouble * 100.0 / total
    List(
      "accepted lines : %d".format(accepted),
      "rejected lines : %d  (%.2f %% of the file)".format(rejected, share)
    )
  }
}
