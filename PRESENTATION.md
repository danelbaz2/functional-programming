# Functional Data Processing Pipeline with Apache Spark

Presentation deck — 5 slides, timed for 3 to 5 minutes.
Speaker notes appear under each slide in a `> Notes:` block.

---

## Slide 1 — E-Commerce Analytics, the Functional Way

**A data pipeline that turns 15,000 raw sales records into business insights**

- **Input:** two external CSV files — 15,000 e-commerce transactions + a 200-product catalog
- **Processing:** Apache Spark (Scala 2.12) — join, filter, and five aggregations
- **Output:** 6 CSV reports + console findings
- **The twist:** every business rule is a *pure function* — Spark only distributes them

> Notes (~40 seconds):
> "Our project is an analytics pipeline for an online shop. It ingests two CSV
> files — fifteen thousand purchase lines and a product catalog — joins them,
> and answers business questions like: which categories make the money, who are
> our best customers, and how many categories generate 80% of revenue.
> The dataset is generated deterministically from a fixed seed, so the same
> seed always rebuilds the exact same 15,000 rows — reproducible by design.
> But the real story of this project isn't *what* it computes — it's *how*.
> Every single business rule is a pure function, and that shaped the entire
> architecture, which brings me to slide two."

---

## Slide 2 — Architecture: Functional Core, Imperative Shell

```
model/   immutable case classes        (Transaction, Product, reports)
core/    PURE functions only           (PureAnalytics — zero Spark imports!)
spark/   distribution decisions        (TransactionJobs — no business rules)
io/      the only impure layer         (files, console, Spark session)
```

- `PureAnalytics.scala` performs **no I/O** and imports **no Spark classes**
- Spark jobs just choose *how to distribute* the pure functions
  (e.g. `groupByKey` → `mapGroups` → `PureAnalytics.foldCategory`)
- **Payoff:** 5 of our 6 ScalaTest suites run *without a Spark session* —
  the whole analytics logic is tested in milliseconds with plain assertions

> Notes (~50 seconds):
> "The architecture is called 'functional core, imperative shell'.
> All the business logic — revenue math, order classification, the Pareto
> analysis — lives in PureAnalytics, a file that imports zero Spark classes
> and touches no file and no console. Every function there depends only on
> its arguments and mutates nothing.
> The Spark layer contains no business rules at all. For example, revenue by
> category is just groupByKey plus mapGroups, and inside the group Spark calls
> our pure foldCategory function. Spark is purely a distribution strategy.
> The payoff is measurable: five of our six test suites don't even start a
> Spark session. Purity here is not a style preference — it's what makes the
> entire pipeline unit-testable."

---

## Slide 3 — Functional Error Handling: Errors Are Data

**We corrupt our own input on purpose — about 1 line in 977 is broken.**

```scala
def parseTransaction(line: String): Either[ParseError, Transaction]
```

- The parser **never throws** — it returns `Either`:
  `Right(transaction)` or `Left(WrongFieldCount / NotANumber / OutOfRange / MissingField)`
- The **same parser is called twice** with two different `flatMap`s:
  - `loadTransactions` keeps the successes → `.toOption` (a `None` just vanishes)
  - `loadRejectedTransactions` keeps the failures → `.left.toOption` → the `rejected_lines` report
- A corrupted line can never crash the run — it becomes a row in the data-quality report

> Notes (~55 seconds):
> "Real data is never clean, so our generator deliberately corrupts roughly one
> line in a thousand — missing fields, negative quantities, prices that aren't
> numbers.
> Our parser never throws an exception. It returns an Either: the right side is
> a parsed transaction, the left side is an immutable error value describing
> exactly what went wrong. Inside, a for-comprehension short-circuits on the
> first failure, and even the exception from toDouble is caught with Try and
> pattern-matched into an ordinary value.
> Then the elegant part: we run the same pure parser twice. One Spark flatMap
> keeps the successes and drops the Nones. A second flatMap keeps only the
> failures, and those become our sixth report — the rejected lines, each with a
> human-readable reason. An error in this pipeline is not an exception that
> unwinds the stack. It's a value that flows through Spark like any other
> record, and the compiler forces every caller to handle both sides."

---

## Slide 4 — Advanced FP Techniques

**1. A custom combinator, built from scratch (not `Function1.andThen`)**

```scala
val pipeline = readRevenue ~> classify ~> labelOf   // Transform[A, B]
enriched.map(t => pipeline.run(t))                  // distributed over the cluster
```

Unit-tested as an *algebra*: `~>` is associative, `identity` is its neutral element.

**2. Tail-recursive Pareto analysis** — `@tailrec` loop, compiler-guaranteed
constant stack; tested on very long lists.

**3. Closures shipped to executors** — a curried pure predicate,
`atLeastRevenue(threshold)`, is partially applied on the driver; Spark
serializes the resulting closure to every executor.

> Notes (~60 seconds):
> "Three advanced techniques to highlight.
> First, our custom combinator. Transform is a function type we wrote from
> scratch — deliberately not reusing Scala's built-in andThen. The tilde-arrow
> operator takes two transformations and returns a new one: a function that
> builds functions. In the order-size job we compose three small steps —
> read the revenue, classify it, label it — into one pipeline, and that single
> composed function is what Spark maps over all 15,000 rows. We even tested its
> algebraic laws: composition is associative and identity is neutral.
> Second, the Pareto analysis is a tail-recursive function annotated with
> @tailrec, so the compiler itself guarantees the recursion compiles to a loop —
> no stack overflow on any input size.
> Third, closures. Our significance filter is a curried pure function. We
> partially apply it with the threshold on the driver, which creates a closure
> over that value, and Spark serializes that closure across the network to the
> executors. Currying isn't decoration here — it's exactly how configuration
> travels to the cluster."

---

## Slide 5 — Business Results & Conclusion

**Six reports written to `output/`:**

`revenue_by_category` · `revenue_by_country` · `top_customers` ·
`order_sizes` · `pareto_categories` · `rejected_lines`

**The headline finding:** the Pareto analysis — a handful of categories
generate **80% of total revenue** (sales follow a Zipf distribution, so the
concentration is real, not an artifact).

**Takeaway:**
- Pure functions made the logic *testable* (~52 tests, mostly Spark-free)
- Immutability made it *safe to distribute*
- Errors-as-values made it *robust against dirty data*

> Notes (~45 seconds):
> "The pipeline ends with six CSV reports — five analytics plus the rejected
> lines. The headline business finding comes from the Pareto report: just a few
> categories account for eighty percent of all revenue, which tells the shop
> where to focus.
> To conclude: functional programming was not an academic constraint we worked
> around — it's what made this pipeline work. Pure functions made the logic
> testable without a cluster. Immutability made it safe to ship across a
> cluster. And treating errors as values made it robust against dirty data.
> The function that found our 80/20 insight is pure, tail-recursive, and
> unit-tested. Thank you — happy to take questions."

---

## Timing cheat-sheet

| Slide | Target | Running total |
|---|---|---|
| 1 — Overview | 0:40 | 0:40 |
| 2 — Architecture | 0:50 | 1:30 |
| 3 — Error handling | 0:55 | 2:25 |
| 4 — Advanced FP | 1:00 | 3:25 |
| 5 — Results | 0:45 | 4:10 |

If running long, trim slide 1 (the overview) — never slide 3 or 4, they carry the grade.

**Likely questions & one-line answers**
- *"Why both Dataset and RDD APIs?"* — The country job deliberately uses the RDD
  API (`map` + `reduceByKey`) to show the classic functional pair; every other
  job uses the typed Dataset API.
- *"How is immutability kept during aggregation?"* — Groups are folded through
  an immutable `RevenueAccumulator`; each step returns a new accumulator.
- *"Is anything random?"* — Yes, but purely: our `Rng` returns
  `(value, nextGenerator)` from every draw — same seed, same dataset, always.
