package com.hit.fp.spark

import org.apache.spark.sql.SparkSession

/*
 * ---------------------------------------------------------------------------
 * SparkSessionProvider.scala
 *
 * Creation of the Spark session used by the pipeline. Building a session is
 * an effect (it starts a local cluster and binds ports), therefore it is kept
 * away from the analytical jobs and confined to this single object.
 *
 * The session runs in local mode, using every core of the machine, so the
 * project can be started from IntelliJ without installing a cluster.
 * ---------------------------------------------------------------------------
 */

/**
 * Builds the Spark session shared by the whole pipeline.
 */
object SparkSessionProvider {

  /** Master of the local cluster, one worker thread per available core. */
  private val localMaster: String = "local[*]"

  /**
   * Builds a Spark session, or returns the one already running.
   *
   * @param applicationName name shown by the Spark user interface
   * @return the session to be used by every job of the pipeline
   */
  def session(applicationName: String): SparkSession = {
    val built = SparkSession
      .builder()
      .appName(applicationName)
      .master(localMaster)
      .config("spark.sql.shuffle.partitions", "8")
      .getOrCreate()
    /* Spark is very verbose by default, only its warnings are useful here. */
    built.sparkContext.setLogLevel("WARN")
    built
  }
}
