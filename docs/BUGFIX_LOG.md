# Bug Fix / Change Log

Every change to this repository follows the same workflow: open a dedicated branch,
apply exactly one logical change, run `sbt compile` and `sbt test` and compare the
outcome with the recorded baseline, commit the change together with its log entry
below, then merge. Entry numbers are stable and are never reused or renumbered.

## Severity scale

- **Critical** — wrong results or a crash of the pipeline.
- **Major** — incorrect edge-case behaviour or silent data loss.
- **Minor** — warnings, doc drift, tooling and build hygiene.

## Entries

| # | Date | Severity | Module / File | Symptom | Root cause | Fix | Verification | Commit |
|---|------|----------|---------------|---------|------------|-----|--------------|--------|
| 1 | 2026-08-27 | Major | repo / `.gitattributes` | On machines without `core.autocrlf`, every tracked text file shows as modified (LF/CRLF churn), burying real diffs and breaking `git blame` | No `.gitattributes`; line-ending behaviour depended on each contributor's local git configuration | Added `.gitattributes`: `* text=auto`, explicit `eol=lf` for `.scala .sbt .md .properties .csv .html`, `.pdf binary`; ran `git add --renormalize .` (no-op — index was already uniformly LF) | `sbt compile` and `sbt test` identical to baseline: 6 suites, 51 tests, all passed; `data/*.csv` byte-identical, reject count unchanged | `chore(repo): normalise line endings via .gitattributes` |
