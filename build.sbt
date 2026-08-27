/*
 * ---------------------------------------------------------------------------
 * build.sbt
 *
 * Build definition of the "Functional Data Processing Pipeline with Apache
 * Spark" project. The project is developed with IntelliJ IDEA, JDK 11 and
 * Scala 2.12.19, exactly as required by the course project document.
 *
 * Dependencies:
 *   spark-core : the RDD API, used by the country revenue job.
 *   spark-sql  : the Dataset / DataFrame API, used by all the other jobs.
 *   scalatest  : the unit testing framework of the pure functional core.
 *
 * The tests are executed in a forked JVM because Apache Spark starts a local
 * cluster and does not release all of its resources inside the sbt JVM.
 * ---------------------------------------------------------------------------
 */

/** Scala version required by the project document. */
scalaVersion := "2.12.19"

/** Version of the deliverable, shared by every module of the build. */
ThisBuild / version := "0.1.0-SNAPSHOT"

/** Scala version shared by every module of the build. */
ThisBuild / scalaVersion := "2.12.19"

/** The single module of the build, rooted in the project directory. */
lazy val root = (project in file("."))
  .settings(
    name := "functional_data_pipeline"
  )

/** Version of Apache Spark used by every Spark job of the pipeline. */
val sparkVersion = "3.3.0"

/** Version of the ScalaTest framework used by the unit tests. */
val scalaTestVersion = "3.2.18"

/*
 * Apache Spark 3.3.0 was built against scala-xml 1.2.0 while ScalaTest was
 * built against scala-xml 2.1.0. The two versions are binary compatible for
 * what both libraries actually use here, so the eviction is accepted instead
 * of being reported as a conflict.
 */
ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

/* Third party libraries needed to compile and to test the project. */
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test

/** Compiler options that keep the functional style of the code honest. */
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding",
  "utf8"
)

/** Spark needs its own JVM when the unit tests are executed. */
Test / fork := true

/*
 * Apache Spark reaches into the internals of the Java platform to manage its
 * off heap memory. Under JDK 11, which this project targets, those internals
 * are still reachable and these options change nothing. They are only needed
 * when the build itself runs on a more recent JDK, where the module system
 * closes them: if sbt is started on a JDK 17 or above, the forked test JVM
 * inherits that JDK unless it is told otherwise.
 */
val sparkModuleOptions = Seq(
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
)

/** The forked test JVM needs the options above to let Spark start. */
Test / javaOptions ++= sparkModuleOptions

/** The pipeline itself is forked too, for the very same reason. */
run / fork := true

/** Options of the JVM the pipeline runs in. */
run / javaOptions ++= sparkModuleOptions

/** Tests never run in parallel, a single local Spark session is shared. */
Test / parallelExecution := false
