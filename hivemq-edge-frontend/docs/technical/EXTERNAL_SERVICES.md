---
title: "External Services"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Master reference for all external dashboards, tools, and services used by the frontend team — URLs, login method, and ownership/access status"
audience: "Team leads, developers onboarding, AI agents"
maintained_at: "docs/technical/EXTERNAL_SERVICES.md"
---

# External Services

---

## Table of Contents

- [Ownership Status Summary](#ownership-status-summary)
- [Monitoring and Analytics](#monitoring-and-analytics)
  - [Heap Analytics](#heap-analytics)
  - [Sentry](#sentry)
  - [Percy](#percy)
- [Code Quality and Security](#code-quality-and-security)
  - [SonarCloud](#sonarcloud)
  - [Snyk](#snyk)
- [CI/CD and Infrastructure](#cicd-and-infrastructure)
  - [Jenkins](#jenkins)
- [Issue and Project Management](#issue-and-project-management)
  - [Linear](#linear)
  - [Kanbanize (Deprecated)](#kanbanize-deprecated)
- [Design and Collaboration](#design-and-collaboration)
  - [Figma](#figma)
  - [Miro](#miro)

---

## Ownership Status Summary

This table tracks whether manager/admin access has been shared with the frontend team lead. Access that is not shared creates a bus-factor risk — only one person can manage the integration.

| Service | Shared with Team Lead | Action Required |
|---------|-----------------------|-----------------|
| Heap Analytics | ✅ Shared | — |
| Sentry | ✅ Shared | — |
| Percy | ❌ Not shared | Share manager access — see [Percy](#percy) |
| SonarCloud | ⚠️ Unknown | Verify and document |
| Snyk | ✅ Shared | — |
| Jenkins | ⚠️ Unknown | Verify and document |
| Linear | ✅ Shared | — |
| Figma | ⚠️ Unknown | Add URL and verify access |
| Miro | ⚠️ Unknown | Add URL and verify access |

---

## Monitoring and Analytics

### Heap Analytics

**Purpose:** Session recording and user behaviour analytics for UX research.

**Dashboard:** [https://heapanalytics.com/app/env/3411251519/overview/usage-baselines](https://heapanalytics.com/app/env/3411251519/overview/usage-baselines)

**Login:** HiveMQ email

**Access status:** ✅ Manager access shared with team lead

**Notes:**

- Project ID `3411251519` is the production environment
- Dev project ID is `1974822562` — use this locally to avoid polluting production data
- Analytics only activate after user accepts the Privacy Consent Banner and the backend's `trackingAllowed` flag is `true`
- User properties captured: `hivemqId`, broker version

**See:** [Configuration — Heap Analytics](./CONFIGURATION.md#heap-analytics)

---

### Sentry

**Purpose:** Error monitoring, performance tracing, and session replay.

**Dashboard:** [https://hivemq.sentry.io/issues/](https://hivemq.sentry.io/issues/)

**Login:** HiveMQ Google SSO

**Access status:** ✅ Manager access shared with team lead

**Notes:**

- Organisation: `hivemq`, Project: `edge`
- Source maps uploaded automatically by `@sentry/vite-plugin` on every build
- Initialises in all environments except `development` — never runs during `pnpm dev`
- Configuration: 100% transaction tracing, 10% session replay, 100% error session replay
- Release tagging: `hivemq-edge@{version}` — requires `VITE_HIVEMQ_EDGE_VERSION` to be set correctly

**See:** [Configuration — Sentry](./CONFIGURATION.md#sentry)

---

### Percy

**Purpose:** Visual regression testing — pixel-level screenshot comparison on every PR.

**Dashboard:** [https://percy.io/f896bbdc/web/hivemq-edge](https://percy.io/f896bbdc/web/hivemq-edge)

**Login:** HiveMQ email

**Access status:** ❌ Manager access NOT yet shared with team lead

> [!WARNING]
> Percy admin access has not been shared with the frontend team lead. If the current token owner leaves the team, the Percy integration will be unmanageable. **Action required:** Share manager/owner access in the Percy project settings.

**Notes:**

- Project token (`PERCY_TOKEN`) is in `.env.local` — do not commit it
- `PERCY_BRANCH=local` prevents local snapshots from polluting the CI baseline
- CI runs Percy in parallel across E2E and component test jobs, coordinated by `PERCY_PARALLEL_NONCE`
- Snapshots are compared against the `master` baseline; changes flag for visual review on the PR

**See:** [Configuration — Percy](./CONFIGURATION.md#percy)

---

## Code Quality and Security

### SonarCloud

**Purpose:** Static analysis, code smell detection, test coverage gate.

**Dashboard (project overview):** [https://sonarcloud.io/project/overview?id=hivemq_hivemq-edge](https://sonarcloud.io/project/overview?id=hivemq_hivemq-edge)

**Dashboard (coverage):** [https://sonarcloud.io/component_measures?id=hivemq_hivemq-edge&metric=coverage](https://sonarcloud.io/component_measures?id=hivemq_hivemq-edge&metric=coverage)

**Login:** GitHub SSO (via HiveMQ organisation)

**Access status:** ⚠️ Unknown — needs verification

**Notes:**

- Analysis runs on every PR and push to `master` via `check-frontend.yml`
- Coverage data is merged from three sources (Vitest + Cypress component + Cypress E2E) before upload
- Quality gate result appears in PR checks and blocks merge on failure
- Use the `/sonarqube` skill to fetch per-PR metrics without leaving the terminal

**See:** [Dependency Management — Snyk](./DEPENDENCY_MANAGEMENT.md#snyk) for the full workflow breakdown.

---

### Snyk

**Purpose:** Dependency vulnerability scanning and ongoing monitoring.

**Dashboard (frontend project):** [https://app.snyk.io/org/hivemq-edge/project/05151663-f435-4653-9e82-98a53a2640d3](https://app.snyk.io/org/hivemq-edge/project/05151663-f435-4653-9e82-98a53a2640d3)

**Login:** HiveMQ SSO (Google)

**Access status:** ✅ Manager access shared with team lead

**Notes:**

- Three GitHub Actions workflows: `snyk-pr.yml` (every PR), `snyk-push.yml` (master monitor), `snyk-release.yml` (production release monitor)
- Frontend scanned separately from backend using `--file=pnpm-lock.yaml`
- Vulnerability alerts currently route to Kanbanize; migration to Linear in progress — see [INT-63](https://linear.app/hivemq/issue/INT-63/snyk-notifier-migrate-to-linear)
- Company-wide tool — configuration is outside the frontend team's control

**See:** [Dependency Management — Snyk](./DEPENDENCY_MANAGEMENT.md#snyk)

---

## CI/CD and Infrastructure

### Jenkins

**Purpose:** Full composite build orchestration (Gradle + frontend), release artifact packaging, and staging environment provisioning.

**Dashboard:** [https://jenkins.cicd.pd.hmq.dev/job/hivemq-edge-commercial-release/job/artifacts/](https://jenkins.cicd.pd.hmq.dev/job/hivemq-edge-commercial-release/job/artifacts/)

**Login:** HiveMQ Okta (VPN required)

**Access status:** ⚠️ Unknown — needs verification

**Notes:**

- Accessed via Okta SSO; not available to external contributors or without HiveMQ VPN
- The `CompositeJenkinsfile` at the monorepo root delegates to the `hivemq-edge-composite` job
- Jenkins builds the `hivemq-edge-{version}.zip` distribution artifact incorporating the frontend
- Also used for staging environment provisioning (Proxmox VM creation) — see [Build and Deployment — Staging](./BUILD_AND_DEPLOYMENT.md#staging--uat-ephemeral-environments)
- Configuration is maintained outside this repository

**See:** [Build and Deployment — Jenkins Integration](./BUILD_AND_DEPLOYMENT.md#jenkins-integration)

---

## Issue and Project Management

### Linear

**Purpose:** Primary issue tracker and sprint management for the frontend team.

**Dashboard:** [https://linear.app/hivemq/](https://linear.app/hivemq/)

**Login:** HiveMQ SSO

**Access status:** ✅ Shared with team

**Notes:**

- Task documentation convention: `.tasks/{LINEAR-ID}-{task-name}/` — see [AI_MANDATORY_RULES.md](../../.github/AI_MANDATORY_RULES.md) RULE 7
- Snyk alerts will route to Linear once [INT-63](https://linear.app/hivemq/issue/INT-63/snyk-notifier-migrate-to-linear) is complete

---

### Kanbanize (Deprecated)

**Purpose:** Former issue tracker — being phased out in favour of Linear.

**Dashboard:** [https://hivemq.kanbanize.com/ctrl_board/57/](https://hivemq.kanbanize.com/ctrl_board/57/)

**Login:** HiveMQ SSO

**Status:** ⚠️ Deprecated — do not create new tickets here

**Pending cleanup:**

- `VITE_APP_ISSUES` in `.env` still points to a Kanbanize board URL — update to Linear once the destination URL is decided
- Snyk alert tags in `snyk-push.yml` still reference Kanbanize board metadata

---

## Design and Collaboration

### Figma

**Purpose:** UI design files, component specifications, and design handoff.

**Dashboard:** URL not yet documented — add here when known

**Login:** HiveMQ email (assumed)

**Access status:** ⚠️ Unknown — URL and access status need to be documented

> [!NOTE]
> If you have the Figma project URL, update this document and add a cross-reference from the [Design Guide](../guides/DESIGN_GUIDE.md).

---

### Miro

**Purpose:** Whiteboarding, architecture diagrams, and team brainstorming.

**Dashboard:** URL not yet documented — add here when known

**Login:** HiveMQ email (assumed)

**Access status:** ⚠️ Unknown — URL and access status need to be documented

> [!NOTE]
> If you have the Miro board URL, update this document and add a cross-reference from the relevant architecture documentation.

---

## Updating This Document

When a new external service is adopted:

1. Add an entry to this document with URL, login method, and initial access status
2. Set access status to ⚠️ Unknown until confirmed
3. Update the [Ownership Status Summary](#ownership-status-summary) table
4. Add a cross-reference from the relevant technical document (Configuration, Dependency Management, etc.)
5. Run `pnpm prettier --write "docs/**/*.md"` before committing
