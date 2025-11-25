# Complete Code Coverage Flow Analysis

## Understanding the Full Coverage Pipeline

Let me walk you through exactly what happens with code coverage for different repository events.

---

## Architecture Overview

### Coverage Collection Points

1. **Vitest (Unit Tests)** → `coverage-vitest/lcov.info`
2. **Cypress E2E** → `coverage-cypress/lcov.info` (E2E target)
3. **Cypress Components** → `coverage-cypress/lcov.info` (Components target)
4. **Cypress Extensions** → `coverage-cypress/lcov.info` (Extensions target)
5. **Cypress Modules** → `coverage-cypress/lcov.info` (Modules target)
6. **Cypress Workspace** → `coverage-cypress/lcov.info` (Workspace target)

### Coverage Aggregation

All LCOV files are collected by the `sonarqube` job:

```yaml
sonarqube:
  needs: [cypress_matrix, unit_tests] # Waits for all tests
  steps:
    - name: Download all LCOV Artifacts
      with:
        pattern: lcov-* # Gets all lcov-* artifacts
        # Results in:
        # - lcov-vitest/lcov.info
        # - lcov-cypress-E2E/lcov.info
        # - lcov-cypress-Components/lcov.info
        # - lcov-cypress-Extensions/lcov.info
        # - lcov-cypress-Modules/lcov.info
        # - lcov-cypress-Workspace/lcov.info
```

---

## Event Flow Analysis

### Event 1: Pull Request - Created (`opened`)

**Trigger:**

```yaml
on:
  pull_request:
    types: [opened, synchronize, reopened] # ← Matches 'opened'
```

#### Step-by-Step Flow:

**1. Workflow Triggered**

```
Event: pull_request (opened)
Context:
  - PR number: 123
  - Source branch: feature/new-component
  - Target branch: main
  - GitHub context available: ✅
```

**2. Build Jobs Run in Parallel**

```
┌─────────────────────┬────────────────────┐
│ build_instrumented  │  build_production  │
│ (VITE_COVERAGE=true)│  (clean)           │
└──────────┬──────────┴────────┬───────────┘
           │                    │
           ↓                    ↓
    application-          application-
    instrumented          clean
```

**3. Test Jobs Execute**

```
unit_tests:
  - Runs: pnpm test:coverage
  - Generates: coverage-vitest/lcov.info
  - Uploads: lcov-vitest artifact
  - Coverage: Unit test coverage ✅

cypress_matrix (5 parallel jobs):
  Job 1 - E2E:
    - Downloads: application-instrumented
    - Runs: pnpm preview + Cypress E2E tests
    - Collects: Istanbul coverage from instrumented build
    - Generates: coverage-cypress/lcov.info
    - Uploads: lcov-cypress-E2E artifact
    - Coverage: E2E coverage ✅

  Job 2 - Components:
    - Uses: Vite dev server (auto-instrumented)
    - Runs: Cypress component tests
    - Collects: Istanbul coverage from dev server
    - Generates: coverage-cypress/lcov.info
    - Uploads: lcov-cypress-Components artifact
    - Coverage: Component coverage ✅

  Job 3 - Extensions:
    - Same as Components
    - Uploads: lcov-cypress-Extensions artifact
    - Coverage: Extension coverage ✅

  Job 4 - Modules:
    - Same as Components
    - Uploads: lcov-cypress-Modules artifact
    - Coverage: Module coverage ✅

  Job 5 - Workspace:
    - Same as Components
    - Uploads: lcov-cypress-Workspace artifact
    - Coverage: Workspace coverage ✅
```

**4. SonarQube Job Aggregates and Analyzes**

```
sonarqube:
  needs: [cypress_matrix, unit_tests]  # Waits for all 6 jobs

  Step 1: Download All Coverage
    - lcov-vitest/lcov.info          ✅
    - lcov-cypress-E2E/lcov.info     ✅
    - lcov-cypress-Components/lcov.info ✅
    - lcov-cypress-Extensions/lcov.info ✅
    - lcov-cypress-Modules/lcov.info    ✅
    - lcov-cypress-Workspace/lcov.info  ✅

  Step 2: SonarQube Scanner Processes
    - Reads: sonar-project.properties
    - Finds: sonar.javascript.lcov.reportPaths (all 6 files)
    - Merges: All coverage data into single analysis
    - Detects: GitHub PR context (PR #123)
    - Mode: PR ANALYSIS (not branch analysis)

  Step 3: Uploads to SonarCloud
    - Project: hivemq_hivemq-edge
    - Analysis type: Pull Request
    - PR key: 123
    - Source branch: feature/new-component
    - Target branch: main
```

**5. SonarCloud Processes PR Analysis**

```
SonarCloud receives:
  - Code changes (from git diff)
  - Combined coverage from all 6 sources
  - PR context (number, branches)
  - Quality gate configuration

SonarCloud analyzes:
  - New code coverage (lines added/changed in PR)
  - Overall project coverage
  - Code smells, bugs, vulnerabilities
  - Code duplication

SonarCloud computes:
  - Coverage on new code
  - Coverage on overall code
  - Quality gate status (pass/fail)
```

**6. GitHub PR Decoration**

```
SonarCloud posts to GitHub PR #123:

  ┌─────────────────────────────────────────────────┐
  │ 📊 SonarCloud Quality Gate: PASSED             │
  ├─────────────────────────────────────────────────┤
  │ Coverage: 78.5% (+2.3%)                         │
  │ New Code Coverage: 82.1%                        │
  │ Bugs: 0                                         │
  │ Code Smells: 3                                  │
  │ Security Hotspots: 0                            │
  │                                                 │
  │ 🔗 View in SonarCloud                          │
  └─────────────────────────────────────────────────┘

Status checks:
  ✅ SonarCloud Code Analysis - Quality gate passed
```

**7. SonarCloud Dashboard Update**

```
SonarCloud UI updates:

  Pull Requests tab:
    ✅ PR #123: "Add new component" appears in list
    - Status: Green (quality gate passed)
    - Coverage: 82.1% (new code)
    - Last analysis: 2 minutes ago

  Activity tab:
    ✅ New analysis recorded
    - Type: Pull Request Analysis
    - PR: #123
    - Timestamp: Nov 25, 2025 10:23 AM
```

---

### Event 2: Pull Request - New Commits (`synchronize`)

**Trigger:**

```
Developer pushes new commits to PR #123
Event: pull_request (synchronize)
```

#### Flow:

**Same as Event 1**, but:

```diff
SonarCloud compares:
- Previous analysis of PR #123
+ New analysis with updated code
= Shows DELTA in coverage

GitHub PR comment UPDATES (not duplicates):
  📊 SonarCloud Quality Gate: PASSED
- Coverage: 78.5% (+2.3%)
+ Coverage: 79.2% (+3.0%)  ← Updated
- New Code Coverage: 82.1%
+ New Code Coverage: 84.5%  ← Updated

  Analysis #2 (updated 1 minute ago)
```

**Key difference:** SonarCloud updates the SAME PR analysis, showing coverage evolution over time.

---

### Event 3: Push to Main Branch (Merge)

**Trigger:**

```
PR #123 is merged to main
Event: push (to main branch)

Workflow triggered by parent's workflow_call
```

#### Flow:

**1. Parent Workflow Triggers**

```yaml
# In check.yml (parent)
on:
  push:
    branches: [main]
    paths:
      - 'hivemq-edge-frontend/**'

jobs:
  frontend:
    uses: ./.github/workflows/check-frontend.yml
```

**2. Same Test Pipeline Runs**

- All 6 coverage sources collected
- SonarCloud analysis triggered

**3. CRITICAL DIFFERENCE - Branch Analysis**

```
SonarCloud receives:
  - Code from main branch
  - Combined coverage
  - NO PR context (this is a push, not a PR)
  - Analysis type: BRANCH ANALYSIS

SonarCloud analyzes:
  - Overall branch coverage
  - Historical trends
  - Quality gate for branch

SonarCloud computes:
  - Main branch coverage: 79.2%
  - Coverage trend: +3.0% from last analysis
  - Quality gate: PASSED
```

**4. NO GitHub PR Decoration**

```
❌ No PR comment (not a PR)
❌ No PR status check (not a PR)
✅ Branch analysis stored
✅ Dashboard updated

SonarCloud Dashboard:

  Branches tab:
    ✅ main branch updated
    - Coverage: 79.2%
    - Quality gate: Passed
    - Last analysis: Just now

  Activity tab:
    ✅ New analysis recorded
    - Type: Branch Analysis
    - Branch: main
    - Coverage: 79.2%
```

---

### Event 4: Pull Request Reopened (`reopened`)

**Trigger:**

```
PR #123 was closed, now reopened
Event: pull_request (reopened)
```

#### Flow:

**Same as Event 1 (opened)**, but:

```
SonarCloud checks:
  - Looks for existing PR #123 analysis
  - Found? Reactivates it
  - Not found? Creates new analysis

Result:
  ✅ PR #123 reappears in Pull Requests tab
  ✅ New analysis run
  ✅ GitHub PR decoration reapplied
  ✅ Fresh coverage computed
```

---

### Event 5: Manual Trigger (`workflow_dispatch`)

**Trigger:**

```
Developer manually triggers workflow from Actions tab
Event: workflow_dispatch
```

#### Flow:

**1. Manual Execution**

```
User selects:
  - Workflow: Frontend - React Testing Pyramid
  - Branch: feature/experimental
  - Click: Run workflow
```

**2. Runs All Tests**

- Same 6 coverage sources collected
- Full pipeline executes

**3. Branch Analysis (Not PR)**

```
SonarCloud receives:
  - Code from selected branch
  - Combined coverage
  - NO PR context (manual trigger)
  - Analysis type: BRANCH ANALYSIS

Result:
  ✅ Branch analysis created
  ❌ No PR decoration
  ✅ Coverage computed for that branch
  ✅ Appears in Branches tab
```

---

### Event 6: Backend-Only PR (No Frontend Changes)

**Trigger:**

```
PR #124 created
Changes: Only backend Java files
Event: pull_request (opened)
```

#### Flow:

**1. Parent Workflow Evaluates**

```yaml
# In check.yml (parent)
on:
  pull_request:
    paths:
      - 'hivemq-edge-frontend/**' # ← Does NOT match

Result: ❌ check-frontend.yml NOT called
```

**2. Frontend Workflow Does NOT Run**

```
❌ No frontend tests
❌ No coverage collected
❌ No SonarCloud analysis for frontend
```

**3. SonarCloud Behavior**

```
SonarCloud for frontend:
  ❌ PR #124 does NOT appear in Pull Requests tab
  ❌ No frontend coverage analysis
  ❌ No GitHub decoration for frontend

(Backend analysis happens separately)
```

---

## Coverage Computation Deep Dive

### How SonarCloud Merges Coverage

**Input:** 6 LCOV files

```
coverage-combined/
  ├── lcov-vitest/lcov.info              (unit test coverage)
  ├── lcov-cypress-E2E/lcov.info         (e2e test coverage)
  ├── lcov-cypress-Components/lcov.info  (component coverage)
  ├── lcov-cypress-Extensions/lcov.info  (extension coverage)
  ├── lcov-cypress-Modules/lcov.info     (module coverage)
  └── lcov-cypress-Workspace/lcov.info   (workspace coverage)
```

**Process:**

```
For each source file (e.g., src/App.tsx):

Step 1: Collect line execution counts from all reports
  - vitest: Line 10 executed 5 times
  - cypress-E2E: Line 10 executed 3 times
  - cypress-Components: Line 10 executed 8 times
  → Merged: Line 10 executed 16 times (5+3+8)

Step 2: Determine coverage per line
  - Line executed > 0 times = COVERED ✅
  - Line executed 0 times = NOT COVERED ❌

Step 3: Calculate file coverage
  - Total lines: 100
  - Covered lines: 85
  - Coverage: 85%

Step 4: Aggregate project coverage
  - Sum covered lines across all files
  - Sum total lines across all files
  - Project coverage = (total covered / total lines)
```

**Result:** Single unified coverage metric combining all test types

---

## Annotation Updates on GitHub

### PR Comment Behavior

**First Analysis:**

```
SonarCloud posts NEW comment on PR
```

**Subsequent Analyses (new commits):**

```
SonarCloud UPDATES existing comment (doesn't create duplicate)

Update mechanism:
  - Finds previous SonarCloud comment
  - Edits comment content
  - Updates metrics
  - Preserves comment URL
```

**Example Evolution:**

```
Analysis #1 (PR opened):
  📊 Coverage: 78.5%

Analysis #2 (new commit):
  📊 Coverage: 79.2% ← Comment UPDATED, not new

Analysis #3 (another commit):
  📊 Coverage: 80.1% ← Same comment UPDATED again
```

### Status Check Behavior

**GitHub Status Checks:**

```
Each push to PR triggers new status check

Status check list:
  ✅ SonarCloud Code Analysis (latest)
  ✅ SonarCloud Code Analysis (from 2 hours ago)
  ✅ SonarCloud Code Analysis (from 1 day ago)

GitHub shows: Most recent status at top
```

---

## Summary Table

| Event                        | Workflow Trigger                         | Coverage Collected | SonarCloud Analysis Type      | GitHub Decoration   | Dashboard Location |
| ---------------------------- | ---------------------------------------- | ------------------ | ----------------------------- | ------------------- | ------------------ |
| **PR Opened**                | ✅ Direct `pull_request`                 | All 6 sources      | **PR Analysis**               | ✅ Comment + Status | Pull Requests tab  |
| **PR Updated (new commits)** | ✅ Direct `pull_request`                 | All 6 sources      | **PR Analysis** (updated)     | ✅ Updates existing | Pull Requests tab  |
| **PR Reopened**              | ✅ Direct `pull_request`                 | All 6 sources      | **PR Analysis** (reactivated) | ✅ Comment + Status | Pull Requests tab  |
| **Merged to Main**           | ✅ Via `workflow_call`                   | All 6 sources      | **Branch Analysis**           | ❌ No (not a PR)    | Branches tab       |
| **Manual Trigger**           | ✅ `workflow_dispatch`                   | All 6 sources      | **Branch Analysis**           | ❌ No (not a PR)    | Branches tab       |
| **Backend-only PR**          | ❌ Not triggered                         | None               | None                          | ❌ No               | Not in dashboard   |
| **PR Title Edit**            | ❌ Not triggered (`edited` not in types) | None               | None                          | ❌ No               | No update          |

---

## Key Insights

### 1. Coverage is Always Combined

Every analysis includes coverage from **all 6 test sources** (unit + 5 Cypress jobs), giving complete coverage picture.

### 2. PR vs Branch Analysis

- **PR Analysis**: Triggered by `pull_request` events → GitHub decoration
- **Branch Analysis**: Triggered by `push` or `workflow_dispatch` → No decoration

### 3. SonarCloud is Smart

- Detects PR context automatically from GitHub events
- Updates existing PR comments (doesn't spam)
- Tracks coverage evolution over time
- Shows delta from target branch

### 4. The `types` Filter Saves Money

By excluding `edited`, `labeled`, etc., we only run tests when code actually changes.

### 5. Path Filtering Prevents Waste

Backend-only PRs don't trigger frontend tests, saving CI minutes.

---

## What You Should See in Practice

### When You Create a PR:

1. ✅ Workflow runs automatically
2. ✅ All tests execute (6 coverage sources)
3. ✅ SonarCloud analyzes PR
4. ✅ Comment appears on PR with coverage metrics
5. ✅ Status check shows in PR checks
6. ✅ PR appears in SonarCloud "Pull Requests" tab

### When You Push New Commits to PR:

1. ✅ Workflow runs again
2. ✅ All tests re-execute
3. ✅ SonarCloud re-analyzes
4. ✅ **Existing comment UPDATES** with new metrics
5. ✅ New status check added (old ones remain in history)
6. ✅ PR analysis in SonarCloud updates

### When PR Merges to Main:

1. ✅ Workflow runs (via parent workflow call)
2. ✅ All tests execute
3. ✅ SonarCloud performs **branch analysis** (not PR)
4. ❌ No PR decoration (it's merged already)
5. ✅ Main branch coverage updates in SonarCloud
6. ✅ Historical trend recorded

This is the complete coverage pipeline! 🎯
