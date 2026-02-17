---
title: "Dependency Management"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Policy and procedures for managing frontend dependencies: update schedule, security tooling, and planned migrations"
audience: "Developers, tech leads, AI agents"
maintained_at: "docs/technical/DEPENDENCY_MANAGEMENT.md"
---

# Dependency Management

---

## Table of Contents

- [Update Policy](#update-policy)
- [Automated Tooling](#automated-tooling)
  - [Renovate](#renovate)
  - [Snyk](#snyk)
- [Acting on Renovate PRs](#acting-on-renovate-prs)
- [Acting on Snyk Alerts](#acting-on-snyk-alerts)
- [Version Pinning Conventions](#version-pinning-conventions)
- [Special Cases](#special-cases)
- [Planned Major Migrations](#planned-major-migrations)

---

## Update Policy

**The team follows a laissez-faire update approach.**

Dependencies are not updated on a scheduled cadence. A version is kept as-is unless one of the following conditions applies:

1. A **security vulnerability** is reported (Snyk alert, GitHub Advisory, CVE)
2. A **bug fix** in a newer version is needed to unblock development
3. A **feature** in a newer version is required for a planned change
4. A **planned major migration** (see [below](#planned-major-migrations)) makes the update necessary

There is no quarterly dependency refresh, no automated merge of Renovate PRs, and no requirement to stay on latest. Stability is preferred over recency.

---

## Automated Tooling

Two external security and update tools are active. Both are managed at the organisation level — their configuration files are **outside this repository** and not accessible to the frontend team directly.

### Renovate

Renovate monitors `package.json` and `pnpm-lock.yaml` for available updates and opens pull requests automatically.

**PR location:** [https://github.com/hivemq/hivemq-edge/pulls?q=is%3Apr+author%3Aapp%2Frenovate](https://github.com/hivemq/hivemq-edge/pulls?q=is%3Apr+author%3Aapp%2Frenovate)

**PR structure:** Renovate splits its PRs by:
- **Scope:** Frontend vs. Backend (separate PRs)
- **Semver level:** Patch, minor, and major updates are separate PRs
- **Labels:** All Renovate PRs carry `renovate`, `edge-team`, and `cla-signed` labels

**Current open Renovate PRs (as of 2026-02-17):**

| PR | Title | Scope |
|----|-------|-------|
| [#1391](https://github.com/hivemq/hivemq-edge/pull/1391) | Update all patch dependencies (master) | All |
| [#1194](https://github.com/hivemq/hivemq-edge/pull/1194) | Update all minor dependencies (master) | All |
| [#1335](https://github.com/hivemq/hivemq-edge/pull/1335) | Update all major GitHub Actions dependencies (major) | CI |
| [#1331](https://github.com/hivemq/hivemq-edge/pull/1331) | Update all non-major GitHub Actions dependencies | CI |

These PRs accumulate over time and are reviewed when relevant — not on a fixed schedule. They are not merged automatically.

---

### Snyk

Snyk provides security vulnerability scanning and monitoring. It is a company-wide tool managed by the HiveMQ security team.

**Dashboard (frontend project):** [https://app.snyk.io/org/hivemq-edge/project/05151663-f435-4653-9e82-98a53a2640d3](https://app.snyk.io/org/hivemq-edge/project/05151663-f435-4653-9e82-98a53a2640d3)

**Alert routing:** Security findings currently route to Kanbanize tickets. Migration to Linear is in progress — see [INT-63](https://linear.app/hivemq/issue/INT-63/snyk-notifier-migrate-to-linear).

**Access and ownership:** See [External Services — Snyk](./EXTERNAL_SERVICES.md#snyk) for dashboard access status and login method.

#### Snyk GitHub Actions Workflows

Three workflows in `.github/workflows/` implement Snyk integration. All are at the **monorepo root** (not inside `hivemq-edge-frontend/`).

**`snyk-pr.yml` — Runs on every PR targeting `master`**

Scans both the backend and the frontend for new issues introduced by the PR:
```yaml
# Frontend scan step (excerpt)
- name: Check for new issues (hivemq-edge-frontend)
  uses: hivemq/hivemq-snyk-composite-action@v2.3.0
  with:
    snyk-args: --org=hivemq-edge -d hivemq-edge/hivemq-edge-frontend
    artifact-name: snyk-report-hivemq-edge-frontend
```

This is the most visible integration — the Snyk check appears in the PR checks list for every pull request. A new high/critical vulnerability introduced by a PR will block or flag the PR.

**`snyk-push.yml` — Runs on push to `master` and on workflow_call**

Runs `snyk monitor` (not just scan) for the development lifecycle. Registers the dependency snapshot against the Snyk project so the dashboard reflects the current `master` state:
```yaml
# Frontend monitor step (excerpt)
- name: Run Snyk Frontend monitor
  run: >
    snyk monitor --file=pnpm-lock.yaml
    --target-reference=${{ github.ref_name }}
    --org=hivemq-edge
    --project-name=hivemq-edge-frontend
    --project-lifecycle=development
    hivemq-edge/hivemq-edge-frontend
```

Tags include Kanbanize board metadata for ticket routing. This will migrate to Linear tags when INT-63 is complete.

**`snyk-release.yml` — Runs on published releases**

Same as push monitoring but uses `--org=hivemq-releases` and `--project-lifecycle=production`. Tracks what is in shipped releases separately from development snapshots.

#### Snyk Scan Summary

| Trigger | Workflow | Action | Org |
|---------|----------|--------|-----|
| Every PR | `snyk-pr.yml` | Scan for new issues; report artifact | `hivemq-edge` |
| Push to master | `snyk-push.yml` | Monitor (snapshot) | `hivemq-edge` |
| Published release | `snyk-release.yml` | Monitor (snapshot, production) | `hivemq-releases` |

---

## Acting on Renovate PRs

Renovate PRs should be reviewed when a dependency update becomes relevant. The process:

1. **Check the PR diff** — Renovate shows the `package.json` and `pnpm-lock.yaml` changes. For patch updates, this is usually a straightforward bump.
2. **Run the full test suite** — `pnpm cypress:run:component` and `pnpm cypress:run:e2e` locally, or let CI run them.
3. **Check for breaking changes** — Minor and major updates may have changelog entries that require code changes. Read the changelog before merging.
4. **Merge if green** — CI passes, no breaking changes: merge. There is no further approval process for patch and minor updates.

For **major version updates**, a migration analysis should be done first (see [Planned Major Migrations](#planned-major-migrations) for examples of what this looks like).

---

## Acting on Snyk Alerts

When Snyk identifies a vulnerability:

1. **Severity assessment** — Critical and High severity findings require action. Medium and Low are tracked but not necessarily acted on immediately.
2. **Check if exploitable** — Snyk often flags vulnerabilities in code paths the application does not use. Assess whether the vulnerable code path is reachable in the frontend context.
3. **Update the dependency** — If exploitable or high severity, update to the fixed version. The update should go through a normal PR with:
   - The dependency bump in `package.json` / `pnpm-lock.yaml`
   - Any code changes required by the updated API (keep these minimal)
   - Full CI pass including Snyk scan on the PR itself

**Example:** [PR #1362](https://github.com/hivemq/hivemq-edge/pull/1362) — security fix for `react-router-dom` (task 38781). Upgraded from 6.11.2 to 6.30.3. Required minimal code changes: a future flag configuration and adjusted redirect logic in the auth flow.

4. **Track via alert system** — Snyk findings route to Kanbanize/Linear tickets (INT-63 covers the migration). Do not resolve a finding without the corresponding code fix.

---

## Version Pinning Conventions

Most dependencies in `package.json` use **exact versions** (no caret or tilde):

```json
"react": "18.3.1",
"@tanstack/react-query": "5.85.5",
"cypress": "15.8.2"
```

This is intentional: exact pins prevent unexpected behaviour when `pnpm install` is run in CI or by a new developer. The lockfile (`pnpm-lock.yaml`) provides a second layer of pinning.

**Caret ranges (`^`) are used for a small number of packages** where the team is comfortable with automatic patch/minor acceptance:

```json
"@dagrejs/dagre": "^1.1.5",
"@monaco-editor/react": "^4.7.0",
"webcola": "^3.4.0",
"@hey-api/openapi-ts": "^0.92.4"
```

These are generally either low-risk layout utilities or tools used only during development/analysis. The lockfile still pins the exact resolved version; the caret only affects what `pnpm update` would consider.

---

## Special Cases

### `xlsx` (SheetJS)

SheetJS is not distributed via npm. It is loaded directly from the SheetJS CDN:

```json
"xlsx": "https://cdn.sheetjs.com/xlsx-0.20.3/xlsx-0.20.3.tgz"
```

This is the [officially supported distribution method](https://docs.sheetjs.com/docs/getting-started/installation/nodejs) for SheetJS Community Edition since the package was removed from npm. Updates require manually changing the version in the URL. Snyk may not scan this dependency via its usual npm resolution — verify coverage on the Snyk dashboard.

### Engine Requirements

The `engines` field enforces minimum runtime versions:

```json
"engines": {
  "node": "22",
  "pnpm": "10"
}
```

These are hard requirements enforced by pnpm. Updates to Node or pnpm versions must be coordinated with CI (`setup_node` GitHub Actions composite action) and any developer tooling documentation.

---

## Planned Major Migrations

Two major dependency migrations have been analysed and are planned but not yet scheduled. Both require dedicated engineering tasks.

### `openapi-typescript-codegen` → `@hey-api/openapi-ts`

**Current:** `openapi-typescript-codegen@0.25.0` — deprecated upstream, no longer receiving fixes.

**Why this matters beyond maintenance:** The current generator cannot produce typed error response schemas from the OpenAPI spec. This is the root cause of the RFC 9457 error handling gap. Switching generators is the prerequisite for typed API error handling.

**Migration complexity:** High. Changes the generated client architecture, enum representation (~174 references), and React Query integration (199 hand-written hooks). See [Problem Detail Analysis](../analysis/PROBLEM_DETAIL_ANALYSIS.md) for the full scope.

Note: `@hey-api/openapi-ts` is already installed as a devDependency (`^0.92.4`) and was used for the feasibility analysis. It is not yet the active generator.

---

### Chakra UI v2 → v3

**Current:** `@chakra-ui/react@2.8.2` — functional but no longer the active release branch.

**Migration complexity:** High. The v3 architecture is a rewrite of the theme system, compound component pattern, and many prop APIs. Affects ~300+ files, the custom theme, and RJSF widget compatibility.

**Constraint:** `@rjsf/chakra-ui` compatibility with Chakra v3 must be confirmed before migration starts.

See [Chakra UI v3 Migration Analysis](../analysis/CHAKRA_V3_MIGRATION.md) for the full cost-benefit analysis and phased plan.
