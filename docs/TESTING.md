# Testing Guide

Testing strategy and TDD implementation for Ensemble. Read [AGENTS.md](../AGENTS.md) first.

## Coverage Split (important)

Strict TDD and the 90% target apply where bugs actually hurt. Do not chase 90% on view plumbing or Terraform.

| Layer | Expectation |
| --- | --- |
| **Backend domain** (storage, tag mapping, stylist agent, guardrails, wear-history) | Strict TDD. ≥90% line; **100% branch** on critical logic. |
| **Controllers / API** | Test request/response contracts and error paths. |
| **Frontend logic** (state, API calls, rendering decisions) | Vitest + RTL on meaningful logic. |
| **Frontend view plumbing** | Light — do not over-test. |
| **Infra / Terraform / CI** | Validate/plan + a smoke deploy check; not unit-tested. |

## Critical Logic Requiring 100% Branch Coverage

- **Grounding / id-validation:** every returned `itemId` must exist; hallucinated ids are rejected; exactly one retry.
- **Forced-output parsing:** `{itemIds, reason}` parsed and validated; malformed output handled.
- **Vision-tag mapping:** vision JSON → item model; tagging failure yields an editable fallback, never a crash.
- **Wear-history rules:** "I wore this" increments `wornCount` + sets `lastWorn`; suggestions reflect recency.

## Test Organization

- **Arrange-Act-Assert** in every test.
- Descriptive names that document behavior, e.g. `styleRequest_withHallucinatedId_retriesOnceThenRejects`.
- Fast, isolated, repeatable.

## Mocking the Claude API

- **Never call the live Claude API in tests.** Mock the Claude client (Mockito) and assert on the request built (tool definitions, that no image bytes go to the stylist) and on how responses are handled.
- Cover: valid structured output, malformed output, hallucinated ids, and API error/timeout.

## DynamoDB Local

- Integration tests use **DynamoDB Local via TestContainers** (or the docker-compose instance).
- Assert real round-trips: create → read → update tags → wear-history update.
- Photo storage tests target the `PhotoStorage` interface with the local-disk impl; the S3 impl gets a thin integration check at deploy.

## Tools

JUnit 5, Mockito, TestContainers, JaCoCo (backend); Vitest, React Testing Library (frontend).
