package com.hit.fp.model

/*
 * ---------------------------------------------------------------------------
 * Transaction.scala
 *
 * Immutable domain model of a single e-commerce transaction. The type is a
 * case class, therefore all of its properties are immutable values and the
 * compiler generates equals, hashCode, toString and copy for free.
 *
 * A case class is also the unit of work of the typed Spark Dataset API: the
 * Spark encoders are able to serialize a case class without any additional
 * code, which lets the whole pipeline stay type safe from the raw CSV line
 * up to the aggregated report.
 *
 * The properties of the class are public on purpose. They are properties (and
 * not setters or getters) of an immutable value object, they carry no state
 * that could be corrupted, and Spark requires access to them in order to
 * build the encoder of the Dataset.
 * ---------------------------------------------------------------------------
 */

/**
 * One purchase of one product, exactly as it is stored in the source file.
 *
 * @param orderId    unique identifier of the order
 * @param customerId identifier of the customer that placed the order
 * @param productId  identifier of the purchased product
 * @param quantity   number of purchased units, always strictly positive
 * @param unitPrice  price of a single unit in US dollars
 * @param country    country the order was shipped to
 * @param orderDate  date of the order, in the ISO format yyyy-MM-dd
 */
final case class Transaction(
    orderId: String,
    customerId: String,
    productId: String,
    quantity: Int,
    unitPrice: Double,
    country: String,
    orderDate: String
)
