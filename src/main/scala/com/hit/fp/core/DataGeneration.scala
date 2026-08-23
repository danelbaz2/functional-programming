package com.hit.fp.core

import java.time.LocalDate

import scala.annotation.tailrec

import com.hit.fp.model.Product
import com.hit.fp.model.Transaction

/*
 * ---------------------------------------------------------------------------
 * DataGeneration.scala
 *
 * Pure generation of the synthetic dataset analysed by the pipeline. The
 * project document allows the dataset to be generated, and generating it
 * keeps the project self contained: no download is needed, and the very same
 * seed always rebuilds the very same files.
 *
 * Nothing here touches the disk. The functions build immutable lists of
 * domain values and render them as CSV lines; writing those lines is the job
 * of the input/output layer, in com.hit.fp.io.DatasetWriter. This is the
 * clear separation between the pure logic and the input/output operations
 * that the project document asks for.
 *
 * The generator deliberately corrupts a few lines of the produced file. Real
 * data is never perfect, and those broken lines are what the functional error
 * handling of CsvParsing is confronted with when the pipeline runs.
 * ---------------------------------------------------------------------------
 */

/**
 * Pure functions building the synthetic dataset of the project.
 */
object DataGeneration {

  /** Header line of the generated transactions file. */
  val transactionHeader: String =
    "orderId,customerId,productId,quantity,unitPrice,country,orderDate"

  /** Header line of the generated products file. */
  val productHeader: String = "productId,productName,category,basePrice"

  /** Commercial categories the generated products belong to. */
  private val categories: Vector[String] =
    Vector(
      "Books",
      "Clothing",
      "Electronics",
      "Garden",
      "Groceries",
      "Sports",
      "Toys",
      "Beauty"
    )

  /** Destination countries of the generated orders. */
  private val countries: Vector[String] =
    Vector(
      "Israel",
      "USA",
      "Germany",
      "France",
      "UK",
      "Italy",
      "Spain",
      "Canada",
      "Japan",
      "Brazil"
    )

  /** First word of the name of a generated product. */
  private val adjectives: Vector[String] =
    Vector("Classic", "Modern", "Compact", "Deluxe", "Urban", "Smart")

  /** Second word of the name of a generated product. */
  private val nouns: Vector[String] =
    Vector("Lamp", "Chair", "Bottle", "Jacket", "Speaker", "Notebook")

  /** Number of distinct customers appearing in the generated orders. */
  private val customerCount: Int = 2000

  /** First day that a generated order can be placed on. */
  private val firstDay: LocalDate = LocalDate.of(2024, 1, 1)

  /** Number of days a generated order can be placed on. */
  private val dayCount: Int = 366

  /** Lowest catalog price of a generated product, in dollars. */
  private val minimalPrice: Double = 5.0

  /** Width of the catalog price range of a generated product, in dollars. */
  private val priceRange: Double = 495.0

  /** Highest number of units a single generated order can hold. */
  private val maximalQuantity: Int = 5

  /** One line out of that many is corrupted on purpose, see [[corrupt]]. */
  private val corruptionPeriod: Int = 977

  /**
   * Exponent of the Zipf law that governs the popularity of a category.
   *
   * Customers do not buy every category evenly: a few of them concentrate
   * most of the sales. Drawing the categories uniformly would produce a flat
   * dataset in which the Pareto analysis has nothing to find, so the draw is
   * weighted instead. The bigger the exponent, the sharper the concentration.
   */
  private val zipfExponent: Double = 1.3

  /**
   * Popularity of every category, in the order of [[categories]].
   *
   * The first category of the vector is the most popular one, and the shares
   * sum up to one.
   */
  private val categoryWeights: Vector[Double] = zipfWeights(categories.length)

  /** Running sums of [[categoryWeights]], used to draw a category. */
  private val categoryCumulative: Vector[Double] = cumulate(categoryWeights)

  /**
   * Builds the catalog of the shop.
   *
   * @param count number of products to build
   * @param rng   generator to draw the random properties from
   * @return the built catalog and the generator to use for the next draw
   */
  def generateProducts(count: Int, rng: Rng): (Vector[Product], Rng) = {
    @tailrec
    def loop(
        index: Int,
        current: Rng,
        built: List[Product]
    ): (Vector[Product], Rng) =
      if (index >= count) (built.reverse.toVector, current)
      else {
        val (adjective, afterAdjective) = current.pick(adjectives)
        val (noun, afterNoun) = afterAdjective.pick(nouns)
        val (category, afterCategory) = afterNoun.pick(categories)
        val (ratio, afterPrice) = afterCategory.nextDouble
        val product = Product(
          productId = productIdOf(index),
          productName = adjective + " " + noun + " " + index,
          category = category,
          basePrice =
            PureAnalytics.roundMoney(minimalPrice + ratio * priceRange)
        )
        loop(index + 1, afterPrice, product :: built)
      }

    loop(0, rng, Nil)
  }

  /**
   * Builds the transactions of the shop.
   *
   * @param count    number of transactions to build
   * @param products catalog the transactions refer to, it must not be empty
   * @param rng      generator to draw the random properties from
   * @return the built transactions and the generator for the next draw
   */
  def generateTransactions(
      count: Int,
      products: Vector[Product],
      rng: Rng
  ): (List[Transaction], Rng) = {
    /* The catalog is indexed once, the loop below only reads that index. */
    val byCategory = products.groupBy(product => product.category)

    @tailrec
    def loop(
        index: Int,
        current: Rng,
        built: List[Transaction]
    ): (List[Transaction], Rng) =
      if (index >= count) (built.reverse, current)
      else {
        val (product, afterProduct) = pickProduct(byCategory, products, current)
        val (customer, afterCustomer) = afterProduct.nextInt(customerCount)
        val (country, afterCountry) = afterCustomer.pick(countries)
        val (units, afterUnits) = afterCountry.nextInt(maximalQuantity)
        val (discount, afterDiscount) = afterUnits.nextDouble
        val (day, afterDay) = afterDiscount.nextInt(dayCount)
        val transaction = Transaction(
          orderId = "T" + "%07d".format(index),
          customerId = "C" + "%05d".format(customer),
          productId = product.productId,
          quantity = units + 1,
          unitPrice = priceOf(product.basePrice, discount),
          country = country,
          orderDate = firstDay.plusDays(day.toLong).toString
        )
        loop(index + 1, afterDay, transaction :: built)
      }

    loop(0, rng, Nil)
  }

  /**
   * Renders a product as a line of the products file.
   *
   * @param product product to render
   * @return the comma separated line describing the product
   */
  def productLine(product: Product): String =
    List(
      product.productId,
      product.productName,
      product.category,
      product.basePrice.toString
    ).mkString(",")

  /**
   * Renders a transaction as a line of the transactions file.
   *
   * @param transaction transaction to render
   * @return the comma separated line describing the transaction
   */
  def transactionLine(transaction: Transaction): String =
    List(
      transaction.orderId,
      transaction.customerId,
      transaction.productId,
      transaction.quantity.toString,
      transaction.unitPrice.toString,
      transaction.country,
      transaction.orderDate
    ).mkString(",")

  /**
   * Renders the whole transactions file, header line included.
   *
   * One line out of [[corruptionPeriod]] is damaged on purpose so that the
   * functional error handling of the pipeline has something to handle. The
   * damaged lines are chosen by their position, so the produced file stays
   * perfectly reproducible.
   *
   * @param transactions transactions to render
   * @return every line of the file, the header line first
   */
  def transactionLines(transactions: List[Transaction]): List[String] = {
    val body = transactions.zipWithIndex.map {
      case (transaction, position) =>
        corrupt(transactionLine(transaction), position)
    }
    transactionHeader :: body
  }

  /**
   * Renders the whole products file, header line included.
   *
   * @param products products to render
   * @return every line of the file, the header line first
   */
  def productLines(products: Vector[Product]): List[String] =
    productHeader :: products.map(productLine).toList

  /**
   * Damages one line out of [[corruptionPeriod]], and keeps the others.
   *
   * @param line     rendered line
   * @param position position of the line in the file
   * @return the damaged line when the position was elected, the line itself
   *         otherwise
   */
  private def corrupt(line: String, position: Int): String =
    if (position > 0 && position % corruptionPeriod == 0) {
      /* The price becomes a word, which NotANumber reports at parse time. */
      val fields = line.split(",", -1)
      fields.updated(4, "N/A").mkString(",")
    } else if (position > corruptionPeriod &&
               position % (corruptionPeriod * 3) == 1) {
      /* A whole field disappears, which WrongFieldCount reports. */
      line.split(",", -1).dropRight(1).mkString(",")
    } else {
      line
    }

  /**
   * Builds the popularity shares of a Zipf law.
   *
   * The element of rank `r` receives a weight proportional to one divided by
   * `r` raised to [[zipfExponent]], and the weights are normalized so that
   * they sum up to one.
   *
   * @param count number of elements to weight
   * @return the share of every element, most popular element first
   */
  private def zipfWeights(count: Int): Vector[Double] = {
    val raw = (1 to count)
      .map(rank => 1.0 / math.pow(rank.toDouble, zipfExponent))
      .toVector
    val total = raw.sum
    raw.map(weight => weight / total)
  }

  /**
   * Turns shares into running sums, the form a draw can be compared to.
   *
   * @param weights shares summing up to one
   * @return the running sums, the last one being one
   */
  private def cumulate(weights: Vector[Double]): Vector[Double] =
    weights.scanLeft(0.0)((sum, weight) => sum + weight).tail

  /**
   * Finds the element a drawn number falls on, with a tail recursion.
   *
   * @param cumulative running sums built by [[cumulate]]
   * @param drawn      number drawn between zero and one
   * @param index      position being examined
   * @return the position the drawn number falls on
   */
  @tailrec
  private def indexFor(
      cumulative: Vector[Double],
      drawn: Double,
      index: Int
  ): Int =
    if (index >= cumulative.length - 1) cumulative.length - 1
    else if (drawn <= cumulative(index)) index
    else indexFor(cumulative, drawn, index + 1)

  /**
   * Draws a category, the popular ones being drawn far more often.
   *
   * @param rng generator to draw from
   * @return the drawn category and the generator for the next draw
   */
  private def pickCategory(rng: Rng): (String, Rng) = {
    val (drawn, next) = rng.nextDouble
    (categories(indexFor(categoryCumulative, drawn, 0)), next)
  }

  /**
   * Draws the product of a purchase.
   *
   * The category is drawn first, following the popularity law, and the
   * product is then drawn uniformly inside that category: the shop stocks
   * every category evenly, but the customers do not buy them evenly.
   *
   * @param byCategory catalog indexed by category
   * @param products   whole catalog, used when a category holds no product
   * @param rng        generator to draw from
   * @return the drawn product and the generator for the next draw
   */
  private def pickProduct(
      byCategory: Map[String, Vector[Product]],
      products: Vector[Product],
      rng: Rng
  ): (Product, Rng) = {
    val (category, afterCategory) = pickCategory(rng)
    afterCategory.pick(byCategory.getOrElse(category, products))
  }

  /**
   * Builds the identifier of the product of the given position.
   *
   * @param index position of the product in the catalog
   * @return the identifier of that product
   */
  private def productIdOf(index: Int): String = "P" + "%04d".format(index)

  /**
   * Applies a discount of at most twenty percent to a catalog price.
   *
   * @param basePrice catalog price of the product
   * @param ratio     drawn number between zero and one
   * @return the price actually paid, rounded to two decimal digits
   */
  private def priceOf(basePrice: Double, ratio: Double): Double =
    PureAnalytics.roundMoney(basePrice * (0.8 + ratio * 0.2))
}
