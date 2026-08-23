package com.hit.fp.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.hit.fp.model.CategoryRevenue
import com.hit.fp.model.EnrichedTransaction
import com.hit.fp.model.Large
import com.hit.fp.model.Medium
import com.hit.fp.model.Premium
import com.hit.fp.model.Product
import com.hit.fp.model.Small
import com.hit.fp.model.Transaction

/*
 * ---------------------------------------------------------------------------
 * PureAnalyticsSpec.scala
 *
 * Unit tests of the functional core: the revenue, the classification, the two
 * tail recursive functions, the generic selection and the Pareto analysis.
 * ---------------------------------------------------------------------------
 */

/**
 * Verifies the analytical rules implemented by the pure core.
 */
class PureAnalyticsSpec extends AnyFunSuite with Matchers {

  /**
   * Builds an enriched transaction with the given category and revenue.
   *
   * @param category commercial category of the built transaction
   * @param revenue  revenue of the built transaction
   * @return the built transaction, its other properties are constants
   */
  private def lineOf(category: String, revenue: Double): EnrichedTransaction =
    EnrichedTransaction(
      orderId = "T1",
      customerId = "C1",
      productName = "Smart Lamp 1",
      category = category,
      country = "Israel",
      quantity = 1,
      unitPrice = revenue,
      revenue = revenue,
      orderDate = "2024-01-01"
    )

  test("the revenue of a transaction is the quantity times the price") {
    val transaction =
      Transaction("T1", "C1", "P1", 3, 19.90, "Israel", "2024-01-01")
    PureAnalytics.revenueOf(transaction) shouldBe 59.70 +- 0.0001
  }

  test("enriching a transaction copies the category of its product") {
    val transaction =
      Transaction("T1", "C1", "P1", 2, 10.0, "Israel", "2024-01-01")
    val product = Product("P1", "Smart Lamp 1", "Garden", 12.0)
    val enriched = PureAnalytics.enrich(transaction, product)
    enriched.category shouldBe "Garden"
    enriched.productName shouldBe "Smart Lamp 1"
    enriched.revenue shouldBe 20.0 +- 0.0001
  }

  test("orders are classified according to their revenue") {
    PureAnalytics.classifyOrder(10.0) shouldBe Small
    PureAnalytics.classifyOrder(49.99) shouldBe Small
    PureAnalytics.classifyOrder(50.0) shouldBe Medium
    PureAnalytics.classifyOrder(199.99) shouldBe Medium
    PureAnalytics.classifyOrder(200.0) shouldBe Large
    PureAnalytics.classifyOrder(999.99) shouldBe Large
    PureAnalytics.classifyOrder(1000.0) shouldBe Premium
  }

  test("every classification has a description") {
    PureAnalytics.describeSize(Small) should not be empty
    PureAnalytics.describeSize(Premium) should not be empty
  }

  test("the curried predicates keep the expected transactions") {
    val rich = lineOf("Books", 120.0)
    val poor = lineOf("Books", 5.0)
    val isSignificant = PureAnalytics.atLeastRevenue(20.0) _
    isSignificant(rich) shouldBe true
    isSignificant(poor) shouldBe false
    val toIsrael = PureAnalytics.shippedTo(Set("Israel")) _
    toIsrael(rich) shouldBe true
    PureAnalytics.shippedTo(Set("Japan"))(rich) shouldBe false
  }

  test("the tail recursive sum handles a very long list") {
    val many = List.fill(200000)(lineOf("Books", 1.0))
    PureAnalytics.totalRevenue(many) shouldBe 200000.0 +- 0.5
  }

  test("the tail recursive sum of an empty list is zero") {
    PureAnalytics.totalRevenue(Nil) shouldBe 0.0 +- 0.0001
  }

  test("folding a group produces its revenue, its lines and its units") {
    val group = Iterator(lineOf("Books", 10.0), lineOf("Books", 15.0))
    val folded = PureAnalytics.foldCategory("Books", group)
    folded.category shouldBe "Books"
    folded.revenue shouldBe 25.0 +- 0.0001
    folded.orders shouldBe 2L
    folded.unitsSold shouldBe 2L
  }

  test("the generic selection keeps the best elements only") {
    val values = List(3.0, 9.0, 1.0, 7.0)
    PureAnalytics.topBy(values, 2)(value => value) shouldBe List(9.0, 7.0)
    PureAnalytics.topBy(values, 10)(value => value).length shouldBe 4
  }

  test("the Pareto analysis cumulates the shares up to one hundred") {
    val categories = List(
      CategoryRevenue("Books", 100.0, 1L, 1L),
      CategoryRevenue("Toys", 300.0, 1L, 1L),
      CategoryRevenue("Sports", 100.0, 1L, 1L)
    )
    val pareto = PureAnalytics.paretoOf(categories)
    pareto.map(line => line.category) shouldBe
      List("Toys", "Books", "Sports")
    pareto.head.cumulativeShare shouldBe 60.0 +- 0.01
    pareto.last.cumulativeShare shouldBe 100.0 +- 0.01
  }

  test("the Pareto analysis of an empty list is empty") {
    PureAnalytics.paretoOf(Nil) shouldBe empty
    PureAnalytics.categoriesUpTo(Nil, 80.0) shouldBe 0
  }

  test("the number of categories reaching a share is counted") {
    val categories = List(
      CategoryRevenue("Books", 100.0, 1L, 1L),
      CategoryRevenue("Toys", 300.0, 1L, 1L),
      CategoryRevenue("Sports", 100.0, 1L, 1L)
    )
    val pareto = PureAnalytics.paretoOf(categories)
    PureAnalytics.categoriesUpTo(pareto, 80.0) shouldBe 2
    PureAnalytics.categoriesUpTo(pareto, 100.0) shouldBe 3
  }

  test("monetary amounts are rounded to two decimal digits") {
    PureAnalytics.roundMoney(1.006) shouldBe 1.01 +- 0.0001
    PureAnalytics.roundMoney(1.0049) shouldBe 1.0 +- 0.0001
  }
}
