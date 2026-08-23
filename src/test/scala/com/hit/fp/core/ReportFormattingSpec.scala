package com.hit.fp.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.hit.fp.model.CategoryRevenue
import com.hit.fp.model.CountryRevenue
import com.hit.fp.model.OrderSize
import com.hit.fp.model.OrderSizeCount

/*
 * ---------------------------------------------------------------------------
 * ReportFormattingSpec.scala
 *
 * Unit tests of the pure rendering functions. They prove that the layout of
 * the final report can be checked without printing anything at all.
 * ---------------------------------------------------------------------------
 */

/**
 * Verifies the pure rendering of the findings of the pipeline.
 */
class ReportFormattingSpec extends AnyFunSuite with Matchers {

  /** Categories shared by the tests of this suite. */
  private val categories: List[CategoryRevenue] = List(
    CategoryRevenue("Books", 100.0, 2L, 3L),
    CategoryRevenue("Toys", 300.0, 5L, 9L)
  )

  test("the categories are rendered richest category first") {
    val lines = ReportFormatting.categoryLines(categories)
    lines.length shouldBe 2
    lines.head should include("Toys")
    lines.last should include("Books")
  }

  test("the countries are rendered richest country first") {
    val lines = ReportFormatting.countryLines(
      List(CountryRevenue("Israel", 10.0), CountryRevenue("Japan", 20.0))
    )
    lines.head should include("Japan")
  }

  test("every classification appears in the distribution report") {
    val lines =
      ReportFormatting.orderSizeLines(List(OrderSizeCount("SMALL", 7L)))
    lines.length shouldBe OrderSize.all.length
    lines.head should include("SMALL")
    lines.head should include("7")
  }

  test("the Pareto report ends with the finding it leads to") {
    val pareto = PureAnalytics.paretoOf(categories)
    val lines = ReportFormatting.paretoLines(pareto, 80.0)
    lines.last should startWith("FINDING")
  }

  test("the quality report shows the share of the rejected lines") {
    val lines = ReportFormatting.qualityLines(99L, 1L)
    lines.head should include("99")
    lines.last should include("1.00 %")
  }

  test("the quality report survives an empty source file") {
    ReportFormatting.qualityLines(0L, 0L).length shouldBe 2
  }

  test("a title is underlined by a separator line") {
    ReportFormatting.title("ANY").length shouldBe 3
  }
}
