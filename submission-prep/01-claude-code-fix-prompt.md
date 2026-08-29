# Claude Code prompt — pre-submission hardening pass

> Paste everything below the line into Claude Code, running with the repository
> root as the working directory.

---

# TASK: Documented pre-submission hardening pass

## 0. Project context

Repository: `functional-programming` — final project for the Functional Programming
course (HIT). Submission deadline: 3 September. This is graded academic work; the
grader reads the source directly, so traceability of every change matters.

Stack: Scala 2.12.19, Apache Spark 3.3.0, target JDK 11, sbt, ScalaTest.

Architecture — **do not violate this boundary, it is the graded centrepiece**:

- `core/` — pure functions only. No I/O, no clock, no global RNG, no exceptions as
  control flow. `PureAnalytics`, `DataGeneration`, `CsvParsing`, `Transform`, `Rng`,
  `ParseError`, `ReportFormatting`.
- `spark/` — distributed jobs: `TransactionJobs`, `SparkSessionProvider`.
- `io/` — effectful edges: `DataSource`, `DataSink`, `DatasetWriter`, `ConsoleReport`,
  `PipelineConfig`.
- `model/` — immutable case classes and sealed ADTs.

The five advanced FP techniques are already implemented AND tagged in comments
(`ADVANCED FUNCTIONAL PROGRAMMING TECHNIQUE 1..5`). **Do not renumber, reword or
remove those tags.** They are how the grader finds the techniques.

Verified baseline facts — do not "fix" these, they are correct by design:
- zero `var` in `src/main` — keep it that way
- `DataGeneration.corrupt` deliberately damages 20 of 15,000 transaction lines
  (15 with a non-numeric price, 5 with a missing field). This is intentional: it is
  what the functional error handling is demonstrated against.
- `data/` is versioned on purpose so the dataset is reproducible.

## 1. Operating rules — binding for the whole task

1. **Plan before you touch anything.** For each step, output the exact change, the
   files it touches, its effect on the rest of the system, and a Low/Medium/High
   output-size estimate. **Stop and wait for explicit approval** ("Proceed" / "Go")
   before writing code. Do this per step, not once for the whole task.
2. **One logical change per commit.** Message format:
   `fix(<module>): <summary>` / `chore(<module>): <summary>` / `docs: <summary>`,
   where `<module>` is one of `core`, `spark`, `io`, `model`, `build`, `repo`.
3. **No opportunistic refactoring.** Do not rename, reorder, reformat or "improve"
   anything outside an approved change. Do not touch dependency versions.
4. **Every change is logged** in `docs/BUGFIX_LOG.md`, in the same commit.
5. **Never leave the build red.** `sbt compile` and `sbt test` must pass before each
   commit. Record the pre-change baseline (test count, pass/fail) before step 3 and
   compare after every step.
6. If a stated precondition does not hold, **stop and report**. Do not improvise.

## 2. Step 1 — Create the change log

Create `docs/BUGFIX_LOG.md`:

- A 3-4 line header describing the workflow: branch -> change -> test -> commit -> log entry.
- A severity scale: **Critical** (wrong results or crash), **Major** (incorrect edge-case
  behaviour, silent data loss), **Minor** (warnings, doc drift, tooling).
- A table with columns:
  `# | Date | Severity | Module / File | Symptom | Root cause | Fix | Verification | Commit`

Commit: `docs: add change log`

## 3. Step 2 — Normalise line endings (Major)

**Symptom:** `git status` reports all 35 tracked files as modified while
`git diff --ignore-all-space --stat` is empty — pure LF/CRLF churn, no content change.
There is no `.gitattributes` and `core.autocrlf` is unset. This buries every real diff
and makes `git blame` useless, which directly undermines the collaborative-tooling
component of the grade.

Branch: `fix/line-endings`

1. **Precondition:** run `git diff --ignore-all-space --stat`. If it is NOT empty,
   abort and report — there are real uncommitted changes to handle first.
2. Record the baseline: `sbt test` output, test count, all suites passing.
3. Create `.gitattributes` at the repository root:
   - `* text=auto` as the default
   - `text eol=lf` for `*.scala`, `*.sbt`, `*.md`, `*.properties`, `*.csv`, `*.html`
   - `*.pdf binary`
4. `git add --renormalize .`
5. Run `sbt compile` then `sbt test`. Test count and outcomes must be identical to
   the baseline. **`data/transactions.csv` and `data/products.csv` are inputs the
   pipeline parses — confirm the reject count is unchanged** (see step 5).
6. Commit: `chore(repo): normalise line endings via .gitattributes`
7. Add the log entry.

## 4. Step 3 — Resolve the sbt version contradiction (Minor, high visibility)

`README.md` instructs the reader to use one sbt version; `project/build.properties`
declares `sbt.version=1.9.9`. A grader following the README hits an inconsistency
before running a single line.

1. Read both files. Determine which version the project actually builds with.
2. Make them agree. Prefer correcting the README over changing the build, unless the
   build genuinely requires the newer sbt — state which you chose and why.
3. While in `README.md`, verify every other setup claim still matches reality
   (JDK version, `HADOOP_HOME` / `winutils` note, `sbt run` / `sbt test` behaviour,
   the record counts it quotes).
4. Commit: `docs: align README with the actual build configuration`

## 5. Step 4 — Pin the bytecode target to JDK 11 (Minor, defensive)

`scalacOptions` currently sets only `-deprecation -feature -unchecked -encoding utf8`.
The build runs on a newer JDK while the project is declared to target JDK 11, so the
emitted bytecode is not pinned. If the grader runs on JDK 11, class-version errors are
possible.

1. Add `"-release", "11"` to `scalacOptions` in `build.sbt`.
2. `sbt clean compile` — if Spark 3.3.0 or any dependency rejects `-release`, fall back
   to `"-target:jvm-1.8"` and say so explicitly rather than leaving it unpinned.
3. `sbt test` must still pass.
4. Commit: `build: pin the bytecode target to JDK 11`

## 6. Step 5 — Close the ScalaDoc gaps (Minor, explicitly graded)

The requirements mandate ScalaDoc in English on functions, objects and classes, and
forbid JavaDoc style. Coverage is good but incomplete. Files with declarations that
carry no doc comment:

- `model/Transaction.scala` — 1 doc comment / 4 declarations
- `core/Transform.scala` — 9 / 17
- `io/ConsoleReport.scala` — 3 / 4
- `io/DataSink.scala` — 2 / 3
- `io/DatasetWriter.scala` — 4 / 5
- `io/DataSource.scala` — 5 / 6
- `core/ParseError.scala` — 11 / 12
- `spark/TransactionJobs.scala` — 9 / 10

For each: add a `/** ... */` ScalaDoc block above every public `def`, `val`, `class`,
`trait`, `object` and `case class` that lacks one, with `@param`, `@return` and
`@tparam` where applicable. English only. Match the existing house style exactly —
read three neighbouring doc comments before writing one. Do not add noise comments
that restate the identifier name; say what the function guarantees.

Commit per file group: `docs(<module>): complete the ScalaDoc coverage`

## 7. Step 6 — Decide the fate of `docs/index.html` (Minor)

`docs/index.html` (~17 KB) entered in a commit named "index.html file for test". It is
a styled overview page of the `com.hit.fp` packages.

Report to me: what it contains, whether anything references it, and whether it is
current with respect to the code. Recommend one of — (a) keep it and document it in the
README as a project overview page, or (b) exclude it from the submission archive.
**Do not delete it without my approval.**

## 8. Step 7 — Verify error-handling coverage (Major if missing)

The lecturer explicitly wants resilience to imperfect data as a highlight. Confirm,
by reading `src/test`, that a test asserts each of:

1. `CsvParsing.parseTransaction` returns `Left(NotANumber(...))` for a non-numeric price
2. `Left(WrongFieldCount(...))` for a line with a missing field
3. `Left(MissingField(...))` for an empty mandatory field
4. `Left(OutOfRange(...))` for a non-positive quantity or price
5. `DataSource.loadTransactions` **drops** rejected lines while
   `loadRejectedTransactions` **captures** them, and the two counts sum to the total
   number of non-header lines

Report which of the five are covered and which are not. **Propose tests for the gaps
but do not write them until I approve.** Any new test goes in the existing suite for
that module, in the existing style (`AnyFunSuite with Matchers`).

## 9. Definition of done

- `docs/BUGFIX_LOG.md` exists and has an entry per change made.
- `.gitattributes` committed; a clean checkout shows no spurious modifications.
- README and `project/build.properties` agree.
- Bytecode target pinned, or an explicit explanation of why it could not be.
- Every public declaration in the files listed in step 6 carries English ScalaDoc.
- A written report on `docs/index.html` and on error-handling test coverage,
  with no unapproved code written for either.
- `sbt clean compile` produces no new warnings; `sbt test` passes with the same
  test count as the recorded baseline.
