package com.hit.fp.model

/*
 * ---------------------------------------------------------------------------
 * EnrichedTransaction.scala
 *
 * Immutable result of joining a transaction with the catalog. The revenue of
 * the line is computed once, when the value is built, and never mutated
 * afterwards. Every aggregation of the pipeline works on this type.
 *
 * A dedicated type (instead of a tuple) keeps the Spark jobs readable and
 * lets the pure functions of the core be documented with meaningful names.
 * ---------------------------------------------------------------------------
 */

/**
 * A transaction enriched with the catalog information of its product.
 *
 * @param orderId     unique identifier of the order
 * @param customerId  identifier of the customer that placed the order
 * @param productName commercial name of the purchased product
 * @param category    commercial category of the purchased product
 * @param country     country the order was shipped to
 * @param quantity    number of purchased units
 * @param unitPrice   price of a single unit in US dollars
 * @param revenue     total revenue of the line, quantity multiplied by price
 * @param orderDate   date of the order, in the ISO format yyyy-MM-dd
 */
final case class EnrichedTransaction(
    orderId: String,
    customerId: String,
    productName: String,
    category: String,
    country: String,
    quantity: Int,
    unitPrice: Double,
    revenue: Double,
    orderDate: String
)
