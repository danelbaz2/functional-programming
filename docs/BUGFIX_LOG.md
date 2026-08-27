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
