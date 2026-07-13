# Architecture Guide

System design and technical decisions for Ensemble. This reflects decisions locked during planning; see the epic (GitHub #1) and `docs/specs/` for per-issue detail.

## Overview

One Spring Boot process serves a JSON API under `/api` and the built React/Vite PWA as static assets — a single deployable container. The app is built **local-first**, then deployed to AWS App Runner.

```
[React/Vite PWA] --/api--> [Spring Boot]
                              |-- WardrobeService --> DynamoDB (items, tags, wear-history)
                              |-- PhotoStorage    --> local disk (dev) | S3 (deploy)
                              |-- TaggingService  --> Claude Haiku 4.5 (vision, upload-time)
                              |-- StylistService  --> Claude Sonnet 5 (tool-loop, searchWardrobe)
```

## The Two AI Jobs

1. **Perception (tagging):** at upload, one Haiku 4.5 vision call turns a garment photo into structured tags (forced JSON). Editable by the user.
2. **Judgment (styling):** on a vibe, Sonnet 5 runs a tool-loop, calls `searchWardrobe`, picks item ids, and writes a reason. **The stylist never receives image bytes** — it reasons over text tags only; the app maps ids → stored photos.

## Data Model (DynamoDB, single-table)

Item:
- `itemId` (partition key), `photoKey`, `createdAt`
- Vision tags: `category`, `primaryColor`, `secondaryColor`, `formality` (1–5), `pattern`, `warmth` (1–3), `descriptors`
- Wear-history: `lastWorn`, `wornCount`

No relational modeling — `searchWardrobe` scans/returns all items at demo scale (~20).

## Photo Storage

`PhotoStorage` interface with two implementations:
- `LocalDiskPhotoStorage` — dev.
- `S3PhotoStorage` — deploy.

Photos are compressed/resized (≤800px JPEG) on save. Code depends only on the interface, so the local→S3 swap is configuration, not a rewrite.

## Stylist Agent + Guardrails

- **Tool:** `searchWardrobe` (no params) returns all items' text tags + wear-history, no images.
- **Output:** forced `{itemIds, reason}`.
- **Grounding:** validate every id exists → reject hallucinated ids → feed the error back → one retry. Only validated ids are rendered.
- **Conversation:** client holds history and resends each turn; the server is stateless. Pushback ("too plain") re-picks and avoids repeating the last look.
- **Wear-history write:** the single "I wore this" action increments `wornCount` and sets `lastWorn`; later suggestions vary based on recency.
- **Deterministic vs LLM:** wear-history (and future weather/color) are tool data, not LLM guesses.

## Scope

- **MVP:** perception + grounded reasoning + wear-history.
- **Stretch (not MVP):** weather, color-as-code, occasion.

## Deployment (later)

- **Compute:** AWS App Runner (one container). No VPC.
- **Data:** S3 (photos) + DynamoDB (items) — reached by the App Runner instance role via IAM, no VPC connector.
- **Secrets:** Claude API key + passcode in AWS Secrets Manager.
- **Pipeline:** Terraform (ECR, App Runner, S3, DynamoDB, IAM, Secrets Manager, OIDC) + GitHub Actions (build → push ECR → deploy). GitHub→AWS auth via OIDC.
- **Frontend:** vite-plugin-pwa generates the manifest + service worker for iPhone home-screen install.

## Security

- Single-user demo: a **user-entered passcode gate**, checked server-side (never shipped in the client bundle).
- A **daily call cap** (~100/day → 429) is the app-side backstop, since no key-level spend cap is available.
- No secrets committed; keys via env (dev) / Secrets Manager (deploy).
