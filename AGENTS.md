# Ensemble — AI Agent Development Guide

This document provides essential guidance for AI agents working on the **Ensemble** AI stylist application.

## Context Marker

Always begin your response with all active emoji markers, in the order they were introduced.

Format: "<marker1><marker2>...\n<response>"

The marker for this instruction is: 🤖

## What Ensemble Is

An AI stylist that dresses you from photos of clothes you own. It is AI-native because it does two jobs a for-loop cannot:

1. **Perception** — a garment photo becomes structured tags via vision (headline advanced feature).
2. **Judgment** — the stylist reasons over messy context (vibe + wear-history) and explains the call.

Delete the LLM and the app dies (it can't tag or reason — only random pairing would survive). **At style-time the LLM never sees images** — it reasons over text tags only, outputs item ids, and the app renders the stored photos.

## Critical Requirement: Strict TDD

**MANDATORY:** All feature implementations must follow Strict Test-Driven Development (TDD):

- **RED Phase:** Write a failing test that defines the desired behavior.
- **GREEN Phase:** Write the minimum code required to make the test pass.
- **REFACTOR Phase:** Improve the code while keeping tests green.

**Never write production code before a failing test.**

**Scope of strict TDD (important for the project timeline):** apply strict TDD to **backend domain logic** — the data/storage layer, vision-tag mapping, the stylist agent and its guardrails, and wear-history rules. For **frontend UI wiring and infrastructure/Terraform glue**, test the meaningful logic but do not chase 90% coverage on view plumbing or IaC. See @docs/TESTING.md for the exact split.

## Documentation Structure

Refer to these guides for detailed information:

@docs/DEVELOPMENT.md — Development Guide: setup, local run, TDD workflow, issue-by-issue SDD process
@docs/TESTING.md — Testing Guide: strategy, coverage split, patterns, mocking the Claude API, DynamoDB Local
@docs/ARCHITECTURE.md — Architecture Guide: system design, data model, tool-loop, guardrails, deploy plan
@docs/PRECOMMIT.md — Pre-commit Guide: hook configuration, usage, troubleshooting

## TDD Standards

### Coverage Requirements

- Minimum **90% line coverage** for new **backend domain** code.
- **100% branch coverage** for critical business logic: grounding / id-validation, forced-output parsing, vision-tag mapping, and wear-history update rules.
- All edge cases must be explicitly tested (empty wardrobe, hallucinated ids, tagging failure, repeated pushback).

### Test Organization

- Follow the Arrange-Act-Assert pattern.
- Use descriptive test names that document behavior.
- Tests must be fast, isolated, and repeatable (no live network calls — mock the Claude API).

### Quality Gates

- Tests written before implementation (RED phase).
- All tests pass before commit.
- Coverage meets standards before merge.

## Code Standards

### Architecture

- **Layered Architecture:** Presentation (controllers) → Business (services) → Data (repositories/storage).
- **Spring Boot best practices:** use starters, follow conventions.
- **Clean Code:** SOLID principles, DRY, single responsibility.

### Data

- **No Spring Data JPA / no relational DB.** Persistence is **AWS SDK v2 DynamoDB Enhanced Client** with a single-item model (`itemId` partition key). There are no entity relationships or cascades.
- Photos are stored via a **`PhotoStorage` interface** — `LocalDiskPhotoStorage` for local dev, `S3PhotoStorage` at deploy. Code depends on the interface, never on S3 directly.
- Use **DTOs** at the API boundary. Never leak the Claude client, DynamoDB items, or storage internals into controllers.

### AI Agent Standards

- Reuse the Claude tool-loop pattern (agent → tool → repository → grounded answer).
- `searchWardrobe` returns **text tags only** — the LLM never receives image bytes.
- The stylist must produce **forced structured output** `{itemIds, reason}`.
- **Grounding guardrail:** validate that every returned `itemId` exists in the wardrobe; reject hallucinated ids, feed the error back, and allow exactly one retry. Never render an unvalidated id.
- Deterministic data (wear-history, and later weather/color) comes from tools — do not ask the LLM to compute it.
- Models: **Haiku 4.5** for vision tagging, **Sonnet 5** for stylist reasoning. Keys come from the environment (local) or Secrets Manager (deploy) — never commit them.

### Frontend

- React 19 + Vite 6, **mobile-first**. Built assets are served by Spring from `/`.
- Test meaningful logic with **Vitest + React Testing Library**; do not over-test view plumbing.

## Development Workflow

1. **Requirements Analysis** — understand the feature and its edge cases (start from the issue + its `docs/specs/` spec).
2. **Test Design** — write comprehensive failing tests.
3. **TDD Implementation** — follow Red-Green-Refactor.
4. **Integration** — verify with existing code.
5. **Documentation** — update relevant docs.

Work **issue-by-issue** using the SDD flow (see @docs/DEVELOPMENT.md). Each issue has a spec in `docs/specs/`.

## Tools and Frameworks

- **Backend testing:** JUnit 5, Mockito, TestContainers (DynamoDB Local), JaCoCo (coverage).
- **Frontend testing:** Vitest, React Testing Library.
- **Build:** Gradle (backend), npm + Vite (frontend), multi-stage Docker (one image serves API + UI).
- **Quality:** Checkstyle + SpotBugs (recommended), pre-commit hooks (see @docs/PRECOMMIT.md).
- **Version Control:** Git with conventional commits.

## Review Checklist

Before committing code:

- [ ] Tests written before implementation
- [ ] All tests pass
- [ ] Coverage meets requirements (>90% backend domain; 100% branch on critical logic)
- [ ] Follows SOLID principles
- [ ] No code duplication
- [ ] Proper error handling (tagging failure, hallucinated ids, empty wardrobe)
- [ ] No secrets committed (Claude key, passcode)
- [ ] Documentation updated

This guide ensures consistent, high-quality TDD practices for AI contributors to the Ensemble AI stylist application.
