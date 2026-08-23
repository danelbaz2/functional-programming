package com.hit.fp.model

/*
 * ---------------------------------------------------------------------------
 * RejectedLine.scala
 *
 * Immutable description of a source line the pipeline refused. Rejected lines
 * are not lost and they never interrupt the run: they are collected in their
 * own Dataset and written to their own report, next to the analytical ones.
 * ---------------------------------------------------------------------------
 */

/**
 * One line of a source file that could not be parsed.
 *
 * @param reason explanation produced by the parsing error
 * @param line   raw line that was rejected
 */
final case class RejectedLine(reason: String, line: String)
