package com.hit.fp.model

/*
 * ---------------------------------------------------------------------------
 * Product.scala
 *
 * Immutable domain model of a product of the catalog. The catalog is the
 * second data source of the pipeline, it is joined with the transactions in
 * order to enrich every transaction with the category of the product it
 * refers to.
 *
 * Keeping the catalog in a separate file (and not denormalized inside every
 * transaction) is what makes the Spark join operation of the pipeline a real
 * join and not an artificial one.
 * ---------------------------------------------------------------------------
 */

/**
 * One product of the e-commerce catalog.
 *
 * @param productId   unique identifier of the product
 * @param productName commercial name of the product
 * @param category    commercial category the product belongs to
 * @param basePrice   catalog price of a single unit in US dollars
 */
final case class Product(
    productId: String,
    productName: String,
    category: String,
    basePrice: Double
)
