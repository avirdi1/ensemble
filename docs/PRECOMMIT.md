# Pre-commit Guide

Local quality gates that run before each commit. Kept intentionally lightweight for the project's timeline — fast checks only, heavy suites run in CI.

## What Runs

On `git commit`, the hook runs (fail = commit blocked):

1. **Backend tests** (fast/unit) — `./gradlew test` (or the fast subset).
2. **Frontend tests** — `cd frontend && npm test -- --run`.
3. **Format + lint** — Checkstyle/SpotBugs (backend), ESLint/Prettier (frontend).
4. **Secret scan** — block accidental commit of the Claude key or passcode.

Full coverage (JaCoCo ≥90% backend domain) and the container build run in CI, not on every commit, to keep commits fast.

## Setup

The repo uses the [`pre-commit`](https://pre-commit.com) framework (or a Git `pre-commit` hook script). Install:

```bash
pip install pre-commit   # or: brew install pre-commit
pre-commit install
```

Configuration lives in `.pre-commit-config.yaml` (added with the tooling for issue #2).

## Usage

- Runs automatically on `git commit`.
- Run manually on all files: `pre-commit run --all-files`.
- Run a single hook: `pre-commit run <hook-id>`.

## Troubleshooting

- **Hook not running:** re-run `pre-commit install`; confirm `.git/hooks/pre-commit` exists.
- **Slow commits:** ensure only fast tests run here; move slow/integration tests to CI.
- **False-positive secret hit:** verify it is not a real credential; if a test fixture, adjust the scanner's allowlist rather than disabling the scan.
- **Need to bypass (rare, discouraged):** `git commit --no-verify` — only for docs-only commits, never to skip failing tests on code.
