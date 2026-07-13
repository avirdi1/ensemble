# 01-spec-app-skeleton.md

## Introduction/Overview

The first slice of the Ensemble MVP: a runnable web-app skeleton. A single Spring Boot process serves a JSON API under `/api` and the built React/Vite frontend as static assets, so the whole app runs as one deployable. This slice establishes the project structure, local dev workflow, and container build that every later feature (storage, vision tagging, stylist agent) plugs into. It contains no business logic — its only job is to prove the plumbing works end to end.

## Goals

- Stand up a Spring Boot backend exposing `GET /api/health` that returns HTTP 200.
- Scaffold a React + Vite mobile-first frontend that calls `/api/health` and shows the result.
- Serve the built frontend from Spring at `/` so one process serves both API and UI.
- Provide a reproducible local dev workflow (backend + frontend, with hot reload for the frontend).
- Provide a multi-stage Docker build that produces one runnable image serving both API and UI.

## User Stories

- **As the developer**, I want a runnable skeleton so that I can add features against a working baseline instead of wiring plumbing later.
- **As the developer**, I want one process serving API + UI so that local dev mirrors the eventual single-container deploy.
- **As the developer**, I want a documented local run so that I (and reviewers) can start the app with one known set of commands.

## Demoable Units of Work

### Unit 1: Backend health endpoint

**Purpose:** Prove the Spring Boot app boots and serves the API.

**Functional Requirements:**
- The system shall start a Spring Boot application locally via a documented command.
- The system shall expose `GET /api/health` returning HTTP 200 with a small JSON body (e.g. `{"status":"ok"}`).

**Proof Artifacts:**
- CLI: `curl -s -o /dev/null -w "%{http_code}" localhost:8080/api/health` returns `200` demonstrates the backend boots and serves the API.
- CLI: `curl -s localhost:8080/api/health` returns the JSON body demonstrates the endpoint contract.

### Unit 2: Frontend scaffold calling the API

**Purpose:** Prove the React/Vite app runs and talks to the backend.

**Functional Requirements:**
- The system shall provide a React + Vite frontend that renders a mobile-first shell.
- The frontend shall call `GET /api/health` on load and display the result (e.g. "ok" / "unreachable").
- The dev setup shall proxy `/api` to the backend during `vite dev`.

**Proof Artifacts:**
- Screenshot: the browser at the vite dev URL shows the health status rendered demonstrates frontend↔backend wiring.

### Unit 3: Single-process serving + container build

**Purpose:** Prove the app ships as one deployable image.

**Functional Requirements:**
- The system shall serve the built frontend static assets from Spring at `/`, with SPA fallback to `index.html` for client routes.
- The system shall provide a multi-stage Dockerfile (node build stage → Java runtime stage) producing one image.
- The image shall run locally and serve both `/` (UI) and `/api/health`.

**Proof Artifacts:**
- CLI: `docker build` succeeds, then `docker run` + `curl localhost:8080/api/health` returns `200` demonstrates the single image serves the API.
- Screenshot: `/` served from the running container renders the UI demonstrates single-process serving.

## Non-Goals (Out of Scope)

1. **No AWS / deploy pipeline** (Terraform, App Runner, ECR, CI) — that is issue #9.
2. **No business logic** (no wardrobe, tagging, or stylist) — later issues.
3. **No PWA install** (manifest / service worker / iOS meta) — issue #8.
4. **No authentication or passcode gate** — issue #8.
5. **No persistence** (DynamoDB / S3 / photo storage) — issue #3.

## Design Considerations

Mobile-first shell: single column, generous touch targets, viewport meta set for mobile. The UI in this slice is a minimal placeholder that only needs to display the health status — just enough surface for later screens (wardrobe grid, add-item, chat) to extend. No design system or theming is required yet; keep the layout neutral.

## Repository Standards

This is a new repository (only `README.md` exists), so this slice establishes standards rather than inheriting them:

- **Backend:** standard Spring Boot project layout under a `com.ensemble` package root, built with Gradle.
- **Frontend:** Vite project in a dedicated directory (e.g. `frontend/`); its build output is copied into Spring's static resources during the build.
- **Docs:** document the local run and build in `README.md`.
- **Commits:** small, reviewable commits (roughly one per demoable unit); conventional-commit-style messages.
- **Ignore rules:** `.gitignore` covers build outputs, `node_modules/`, and local env files.

## Technical Considerations

- **Backend:** Java 21 (LTS), Spring Boot 3.4.x (current stable line), Gradle.
- **Frontend:** React 19 + Vite 6, npm.
- **Serving:** Spring serves static assets from the classpath (e.g. `src/main/resources/static`) with an SPA fallback so client-side routes resolve to `index.html`. API routes live under `/api/**` and must not be shadowed by the SPA fallback.
- **Local dev:** run Spring (`bootRun`, port 8080) and `vite dev` (port 5173) together; the vite dev server proxies `/api` → `localhost:8080`. In the container / production build, a single port (8080) serves both UI and API.
- **Build wiring:** the frontend build is wired into the Gradle build (or the Dockerfile) so that `docker build` produces the combined artifact with no manual copy step.
- **Multi-stage Dockerfile:** stage 1 (node) builds the frontend; the Gradle build packages the jar with the static assets; the final stage runs on a slim JRE image.

## Security Considerations

- No secrets are introduced in this slice (no Claude API key yet) — ensure no credentials are committed.
- The health endpoint returns no sensitive data.
- `.gitignore` must exclude build outputs, `node_modules/`, and any local env files to avoid accidental commits.

## Success Metrics

1. `curl /api/health` returns `200` locally (backend up).
2. The frontend renders the health status obtained from the API in a browser.
3. `docker build` produces one image that serves both UI and API, verified via `curl` + browser.
4. A new developer can start the app following only the `README.md`.

## Open Questions

1. **Build tool:** Gradle is assumed (vs Maven) — non-blocking; swap if preferred.
2. **Versions:** Java 21 / Spring Boot 3.4.x / React 19 / Vite 6 are assumed current; confirm/bump exact patch versions at implementation time — non-blocking.
3. **Frontend directory name:** `frontend/` assumed (vs `web/`) — non-blocking naming choice.
4. **Package manager:** npm assumed (vs pnpm/yarn) — non-blocking.
