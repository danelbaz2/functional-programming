# Functional Data Processing Pipeline with Apache Spark

[![Scala](https://img.shields.io/badge/Scala-2.12-DC322F?logo=scala&logoColor=white)](https://www.scala-lang.org/)
[![sbt](https://img.shields.io/badge/sbt-1.9.9-380D09?logo=scala&logoColor=white)](https://www.scala-sbt.org/)
[![Apache Spark](https://img.shields.io/badge/Apache%20Spark-3.3-E25A1C?logo=apachespark&logoColor=white)](https://spark.apache.org/)
[![JDK](https://img.shields.io/badge/JDK-11-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/11/)
[![Tests](https://img.shields.io/badge/tests-ScalaTest-success)](src/test/scala/com/hit/fp)

## Project Overview

An end-to-end e-commerce analytics pipeline written in **Scala** on **Apache
Spark**. It shows how a distributed data job can be built around a **purely
functional core**: all business rules are side-effect-free functions, and
every file read, file write and console print is isolated in a thin I/O layer.

The pipeline:

- ingests **15,000 transactions** and a **200-product catalog** from CSV
  (deterministically generated from a fixed seed, so every machine sees the
  same data),
- **validates every line without throwing exceptions**. Corrupted records are
  captured as typed errors and routed to their own report,
- **joins, filters and aggregates** the data with the Spark Dataset and RDD
  APIs,
- produces **six CSV reports** and a printed summary of the findings.

**The problem it solves:** analytics code often mixes business logic with
Spark sessions and I/O, which makes the code hard to test and to reason about.
In this pipeline, every analytical rule can be unit tested in plain Scala,
without starting a Spark cluster.

> A visual overview of the architecture is available in
> [docs/index.html](docs/index.html). Open it in a browser.

## Architecture & Data Flow

The code follows a **functional core / imperative shell** design:

| Package | Purity | Responsibility |
|---|---|---|
| [`model`](src/main/scala/com/hit/fp/model) | pure | Immutable case classes and the sealed `OrderSize` ADT |
| [`core`](src/main/scala/com/hit/fp/core) | **pure** | Parsing, analytics, data generation, report rendering, the `Transform` combinator |
| [`spark`](src/main/scala/com/hit/fp/spark) | pure transformations | Distributes the core functions across Spark |
| [`io`](src/main/scala/com/hit/fp/io) | **effects** | The only code that touches the filesystem or the console |
| [`Main`](src/main/scala/com/hit/fp/Main.scala) | effects | The impure shell that wires one run together |

```
 ┌──────────────────┐   ┌──────────────────┐   ┌───────────────────────────┐
 │ 1. Generate      │──▶│ 2. Ingest        │──▶│ 3. Transform (Spark)      │
 │ DatasetWriter    │   │ DataSource       │   │ TransactionJobs           │
 │ seeded pure RNG  │   │ CSV → Either     │   │ joinWith · filter · group │
 └──────────────────┘   │ accepted/rejected│   │ reduceByKey · Pareto      │
                        └──────────────────┘   └─────────────┬─────────────┘
                                                             ▼
                        ┌──────────────────┐   ┌───────────────────────────┐
                        │ 5. Report        │◀──│ 4. Persist                │
                        │ ConsoleReport    │   │ DataSink → output/*.csv   │
                        └──────────────────┘   └───────────────────────────┘
```

1. **Generate:** if `data/` is missing, a purely functional, seeded random
   generator (`core.Rng`) rebuilds the dataset with identical bytes. About
   one line in a thousand is corrupted on purpose, so the error handling runs
   on real bad input every time.
2. **Ingest:** `DataSource` reads both CSV files. Each line is parsed by
   `CsvParsing` into an `Either[ParseError, Transaction]`, which splits the
   input into accepted records and rejected lines.
3. **Transform:** `TransactionJobs` enriches transactions with the product
   catalog (`joinWith`), keeps significant orders and aggregates them by
   category, country, customer and order size. It also runs a Pareto analysis
   (80/20) on category revenue.
4. **Persist:** `DataSink` writes six reports to `output/`: revenue by
   category, revenue by country, top 20 customers, order-size distribution,
   Pareto categories and rejected lines.
5. **Report:** the pure `ReportFormatting` module renders the findings, and
   `ConsoleReport` prints them.

All paths and thresholds live in one immutable `PipelineConfig`, so a test
can run the whole pipeline against a temporary directory by passing a
different value.

## Technical Highlights & Contributions

This was a **collaborative academic project, completed with my partner Dan
Elbaz**, for a Functional Programming course.

### Functional programming in practice

- **Pure functions throughout the core.** Nothing in `core` reads a file,
  writes a file, prints or mutates state. The code uses only `val` and
  immutable `case class` values.
- **Errors as values.** `CsvParsing` returns `Either[ParseError, _]` over a
  sealed error hierarchy. No exceptions are thrown for bad input, and rejected
  lines become their own report.
- **Algebraic data types and pattern matching.** Orders are classified into a
  sealed `OrderSize` ADT (`SMALL` / `MEDIUM` / `LARGE` / `PREMIUM`), and the
  compiler checks that every match is exhaustive.
- **Custom composable combinator.** `core.Transform` provides `~>`, `zip`,
  `filterK` and `Transform.chain`. Unit tests check its composition laws, and
  the Spark order-size job is built from it.
- **Stack-safe recursion.** Revenue totals, the Pareto computation and data
  generation are `@tailrec` functions.
- **Currying and higher-order functions.** Examples include
  `PureAnalytics.atLeastRevenue`, `shippedTo`, `CsvParsing.nonEmptyField` and
  the generic `topBy`.
- **Referentially transparent randomness.** `core.Rng` passes its state from
  call to call instead of mutating it, which makes generated data reproducible.

### Apache Spark for analytics

| Spark operation | Used in |
|---|---|
| `joinWith` (typed Dataset join) | `TransactionJobs.enrich`: transactions × product catalog |
| `map` / `filter` / `flatMap` | Enrichment, significance filtering, CSV ingestion |
| `groupByKey` + `mapGroups` | `revenueByCategory`, `spendingByCustomer`, `orderSizeDistribution` |
| `reduceByKey` (RDD API) | `revenueByCountry` |
| `persist(MEMORY_AND_DISK)` | Caches the enriched dataset that several jobs reuse |
| Closures shipped to executors | `keepSignificant` captures its threshold from `PipelineConfig` |

The pipeline uses both the typed **Dataset API** and the lower-level
**RDD API**. In both, the logic inside each transformation is a call to a
pure function from `core`.

### My contributions

- **Build reliability:** limited the compiler to the JDK 11 API surface
  (`-release 11`), so the build cannot depend on newer JDK APIs. Aligned the
  documentation with the actual build configuration.
- **Test coverage:** added `DataSourceSpec`, which runs on a local Spark
  session and checks that accepted and rejected lines exactly partition a
  source file.
- **Code documentation:** completed ScalaDoc coverage of the pure functional
  core.
- **Repository hygiene and delivery:** normalized line endings across
  platforms (`.gitattributes`), kept the change log, corrected the project
  overview page and prepared the presentation materials.

### Testing

| Suite | Covers |
|---|---|
| `core.CsvParsingSpec` | Every accepted and rejected shape of a source line |
| `core.PureAnalyticsSpec` | Revenue, classification, tail recursion, `topBy`, Pareto |
| `core.TransformSpec` | The custom combinator and its laws |
| `core.DataGenerationSpec` | Reproducibility and validity of the generated data |
| `core.ReportFormattingSpec` | Rendering of every report section |
| `io.DataSourceSpec` | Accepted/rejected split of a source file on a local Spark session |
| `spark.TransactionJobsSpec` | Every Spark job, on a local session and a small dataset |

## Quick Start

### Prerequisites

- **JDK 11** (the project target. sbt can also run on JDK 17+, and `build.sbt`
  already passes the `--add-opens` flags Spark needs there)
- **sbt 1.9.9** (the version is pinned in `project/build.properties`, and the
  launcher downloads it automatically)

Scala 2.12.19, Spark 3.3.0 and ScalaTest are downloaded by sbt on the first
build. Expect a few hundred MB on the first build.

### Build, test and run

```bash
# 1. Clone the repository
git clone <repository-url>
cd functional-programming

# 2. Compile
sbt compile

# 3. Run the unit tests (forked JVM, local Spark session)
sbt test

# 4. Run the pipeline
sbt run
```

`sbt run` uses the CSV files in `data/` (regenerating them if the folder is
missing), writes the reports to `output/` and prints a summary like:

```
SOURCE FILE QUALITY
REVENUE BY CATEGORY
REVENUE BY COUNTRY
TOP CUSTOMERS
ORDER SIZE DISTRIBUTION
PARETO ANALYSIS OF THE CATEGORIES
TOTAL
```

To get a fresh dataset, delete `data/` and run again. The fixed seed in
`PipelineConfig.default` produces identical files.

### IntelliJ IDEA

Open the folder as an sbt project, set the project SDK to JDK 11
(`File → Project Structure → Project`), and run `com.hit.fp.Main`.

### Windows only: Hadoop native helper

Spark writes to the local disk through Hadoop, which needs `winutils.exe` on
Windows. Without it, reading works but writing reports fails with
`HADOOP_HOME and hadoop.home.dir are unset`. One-time setup:

1. Download `winutils.exe` and `hadoop.dll` for Hadoop 3.3.x (for example
   from the `cdarlint/winutils` repository) into `C:\Users\<you>\hadoop\bin`.
2. Set the user environment variable `HADOOP_HOME=C:\Users\<you>\hadoop` and
   add `%HADOOP_HOME%\bin` to `PATH`.
3. Restart your terminal or IDE so it picks up the new environment.

Linux and macOS need no extra setup.
