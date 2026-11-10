# Functional Data Processing Pipeline with Apache Spark

Final project in Functional Programming — a complete analytics pipeline written
in Scala 2.12.19 on Apache Spark 3.3.0, running on JDK 11.

The pipeline generates a synthetic e-commerce dataset (15,000 transactions and
200 products, well above the 10,000 records required), reads it back from the
disk, joins and aggregates it with Spark,
and writes six reports plus a printed summary of the findings.

`docs/index.html` is a standalone overview page of the project: open it in a
browser for a visual summary of the architecture, the Spark operations, the
functional techniques and the setup.

## How to run

```
sbt run          # generates the dataset if missing, then runs the pipeline
sbt test         # runs the ScalaTest suites
```

In IntelliJ: open the folder as an sbt project, set the SDK to JDK 11, and run
`com.hit.fp.Main`.

Generated input goes to `data/`, reports go to `output/`. Deleting `data/`
regenerates it on the next run; the seed in `PipelineConfig.default` makes the
generated dataset identical on every machine.

## Setting up on a new machine

Install these yourself, they are not part of the repository:

1. **JDK 11** — the project SDK, as required by the course.
2. **No second JDK is needed** — sbt 1.9.9, the version declared in
   `project/build.properties`, starts fine on JDK 11. If sbt runs on a newer
   JDK (17 or above), `build.sbt` already passes the `--add-opens` flags the
   forked Spark JVMs need there.
3. **IntelliJ IDEA** with the Scala plugin.
4. **Windows only**: `winutils.exe` and `HADOOP_HOME`, see the section below.

Everything else is downloaded automatically the first time the project is
imported: sbt itself, the Scala 2.12.19 compiler, Spark 3.3.0 with its whole
dependency tree, and ScalaTest. Expect a few hundred megabytes and several
minutes on the first import, then nothing.

In IntelliJ, set the project SDK to the JDK 11 under
`File -> Project Structure -> Project`; the sbt JVM under
`Settings -> Build Tools -> sbt -> JVM` can stay on JDK 11 or any newer JDK.

The dataset is versioned in `data/`, so there is nothing to generate: the
pipeline reads the very same 15,000 transactions on every machine. Deleting
`data/` makes the generator rebuild it, identical to the byte thanks to its
fixed seed.

## Running on Windows

Spark reaches the local disk through Hadoop's filesystem layer, and Hadoop
needs a small native helper to set file permissions the Windows way. Without
it, reading works but writing the reports fails with:

```
HADOOP_HOME and hadoop.home.dir are unset
```

Fix it once, outside of the project:

1. Download `winutils.exe` and `hadoop.dll` for Hadoop 3.3.x (for instance
   from the `cdarlint/winutils` repository) into a folder such as
   `C:\Users\<you>\hadoop\bin`.
2. Set the user environment variable `HADOOP_HOME` to `C:\Users\<you>\hadoop`
   and add `%HADOOP_HOME%\bin` to the user `PATH`.
3. Restart IntelliJ completely, otherwise it keeps the old environment.

Linux and macOS need none of this.

## Architecture — pure logic separated from I/O

| Package | Purity | Responsibility |
|---|---|---|
| `com.hit.fp.model` | pure | immutable case classes and the `OrderSize` ADT |
| `com.hit.fp.core` | **pure** | parsing, analytics, generation, rendering, combinator |
| `com.hit.fp.spark` | pure transformations | how the core functions are distributed |
| `com.hit.fp.io` | **effects** | the only code that reads files, writes files, prints |
| `com.hit.fp.Main` | effects | the impure shell that wires the run together |

No function in `core` reads a file, writes a file or prints. That is why every
analytical rule is unit tested without a Spark session.

## Spark operations used

| Operation | Where |
|---|---|
| `joinWith` | `TransactionJobs.enrich` — transactions joined with the catalog |
| `map` | `TransactionJobs.enrich`, `TransactionJobs.orderSizeDistribution` |
| `filter` | `TransactionJobs.keepSignificant`, `DataSource.readLines` |
| `flatMap` | `DataSource.loadTransactions`, `DataSource.loadProducts` |
| `groupByKey` + `mapGroups` | `TransactionJobs.revenueByCategory`, `spendingByCustomer` |
| `reduceByKey` (RDD API) | `TransactionJobs.revenueByCountry` |

Both the typed Dataset API and the RDD API are used; `Main` loads from two
external CSV files and writes every report back to `output/`.

## Advanced functional programming techniques

| Technique | Where |
|---|---|
| Custom combinator | `core.Transform` — `~>`, `zip`, `filterK`, `Transform.chain`; used in `TransactionJobs.orderSizeDistribution` |
| Closures in Spark transformations | `TransactionJobs.keepSignificant` captures the threshold from `PipelineConfig` and ships it to the executors |
| Tail recursion | `PureAnalytics.totalRevenue`, `PureAnalytics.paretoOf`, `DataGeneration.generateProducts`, `DataGeneration.generateTransactions` (all `@tailrec`) |
| Pattern matching with case classes | `PureAnalytics.classifyOrder` and `PureAnalytics.describeSize` over the sealed `OrderSize` ADT; `CsvParsing.positiveNumber` over `Try` |
| Functional error handling | `core.ParseError` + `CsvParsing` return `Either` — no exception is ever thrown for a corrupted line, rejected lines flow into their own report |

Supporting principles: currying (`PureAnalytics.atLeastRevenue`,
`shippedTo`, `CsvParsing.nonEmptyField`), higher-order and generic functions
(`PureAnalytics.topBy`), immutability everywhere (only `case class` and `val`),
and a purely functional pseudo random generator (`core.Rng`) that threads its
state instead of mutating it.

## Findings the run prints

* revenue, order count and units sold per category
* revenue per destination country (computed on the RDD API)
* the 20 biggest spending customers
* the distribution of order sizes (SMALL / MEDIUM / LARGE / PREMIUM)
* a Pareto analysis: how many categories generate 80% of the revenue
* the health of the source file: accepted vs rejected lines

The generator damages roughly one line in a thousand on purpose, so the
functional error handling has something real to handle at every run.

## Tests

| Suite | Covers |
|---|---|
| `core.CsvParsingSpec` | every accepted and rejected shape of a source line |
| `core.PureAnalyticsSpec` | revenue, classification, tail recursion, `topBy`, Pareto |
| `core.TransformSpec` | the custom combinator and its laws |
| `core.DataGenerationSpec` | reproducibility and validity of the generated data |
| `core.ReportFormattingSpec` | the rendering of every section of the report |
| `spark.TransactionJobsSpec` | every Spark job, on a local session and a tiny dataset |
