---
name: sonarqube
description: Fetch and analyze SonarQube quality metrics for pull requests, reporting on code quality, bugs, vulnerabilities, coverage, and code smells
argument-hint: [PR number|branch] [--detailed] [--quality-gate-only]
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Grep, Read, WebFetch
---

# SonarQube Analysis Skill

Automatically fetches and analyzes SonarQube quality metrics for pull requests, providing insights on code quality, test coverage, bugs, vulnerabilities, and technical debt.

## Prerequisites & Hooks

This skill requires several conditions to be met before execution:

### Hook 1: PR Existence Check

**Objective:** Verify a pull request exists for the current branch

**Commands:**

```bash
# Get current branch
git branch --show-current

# Check if PR exists
gh pr view --json number,title,state,url
```

**Failure handling:**

- If no PR exists: Provide instructions to create one first
- If PR is closed: Report status and ask if analysis is still needed
- If no gh CLI: Fallback to manual PR number input

**Example output:**

```
✅ PR #1386 found: "feat: Frontend Ownership Tracking for Data Combiners"
   Status: OPEN
   URL: https://github.com/hivemq/hivemq-edge/pull/1386
```

---

### Hook 2: GitHub Actions Status Check

**Objective:** Ensure SonarQube analysis GitHub Action has completed

**Commands:**

```bash
# Get PR number from Hook 1
PR_NUMBER=$(gh pr view --json number -q .number)

# Check GH Actions status for this PR
gh pr checks $PR_NUMBER --json name,conclusion,status,detailsUrl

# Filter for SonarQube-related workflow
gh pr checks $PR_NUMBER --json name,conclusion,status,detailsUrl | \
  jq '.[] | select(.name | contains("SonarQube") or contains("sonarcloud") or contains("Code Quality"))'
```

**Expected workflow names:**

- "SonarQube Analysis"
- "SonarCloud Scan"
- "Code Quality"
- (configurable in config.yaml)

**Statuses:**

- ✅ `conclusion: "success"` → Proceed to Hook 3
- ⏳ `status: "in_progress"` → Wait or report estimated time
- ❌ `conclusion: "failure"` → Report failure, show logs URL, ask if should continue anyway
- 🔴 `conclusion: "cancelled"` → Report cancellation, suggest re-run

**Failure handling:**

- If action not found: Check config for workflow name
- If action failed: Show details URL, ask user if should continue with stale data
- If action in progress: Report estimated completion time, offer to wait

**Example output:**

```
✅ SonarQube Analysis: PASSED
   Duration: 2m 34s
   Completed: 5 minutes ago
   Details: https://github.com/hivemq/hivemq-edge/actions/runs/12345
```

---

### Hook 3: SonarQube PR Data Availability

**Objective:** Verify SonarQube has analysis data for this PR

**API Endpoint:**

```
GET https://sonarcloud.io/api/measures/component
  ?component={project_key}
  &pullRequest={pr_number}
  &metricKeys=alert_status,bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density
```

**Fallback if PR data not found:**

- Check branch analysis instead
- Report that PR-specific analysis is pending
- Suggest re-running GH workflow

**Configuration (from config.yaml):**

```yaml
sonarcloud:
  base_url: https://sonarcloud.io
  organization: hivemq
  project_key: hivemq_hivemq-edge
  api_version: v1
```

**Failure handling:**

- If 404: SonarQube hasn't analyzed this PR yet
- If 401: Authentication issue (report, continue with public data)
- If 500: SonarQube service issue (report, suggest retry)

**Example output:**

```
✅ SonarQube data available for PR #1386
   Analysis date: 2026-02-10 14:32 UTC
   New code period: 2026-02-09 to 2026-02-10
```

---

## Usage

### Basic Usage

```bash
# Analyze current PR (auto-detect)
/sonarqube

# Analyze specific PR by number
/sonarqube 1386

# Analyze specific branch
/sonarqube feature/new-feature
```

### Advanced Options

```bash
# Detailed report (includes file-level metrics)
/sonarqube --detailed

# Quality gate only (quick check)
/sonarqube --quality-gate-only

# Force analysis (bypass hooks)
/sonarqube 1386 --force
```

### Arguments

- `[PR number]`: PR number (e.g., `1386`) - auto-detected if omitted
- `[branch]`: Branch name (e.g., `feature/new-feature`) - auto-detected if omitted
- `--detailed`: Include file-level hotspots, detailed metrics
- `--quality-gate-only`: Show only pass/fail status
- `--force`: Skip Hook 1 and Hook 2 (go directly to Hook 3)

---

## Skill Workflow

### Phase 1: Prerequisites Check (Hooks)

Execute Hook 1, Hook 2, Hook 3 in sequence. If any fail:

- Report clear error message
- Provide actionable fix instructions
- Ask user if should continue anyway (with `--force`)

**Skip with:**

```bash
/sonarqube --force  # Skip PR and GH action checks
```

---

### Phase 2: Fetch SonarQube Metrics

#### 2.1 Quality Gate Status

**API Call:**

```bash
curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=hivemq_hivemq-edge&pullRequest=${PR_NUMBER}"
```

**Extract:**

- Overall status: `PASSED` / `FAILED` / `ERROR`
- Conditions: Each metric and its status
- Failing conditions: Which metrics failed the gate

**Parse JSON:**

```json
{
  "projectStatus": {
    "status": "OK",
    "conditions": [
      {
        "status": "OK",
        "metricKey": "new_coverage",
        "comparator": "LT",
        "errorThreshold": "80",
        "actualValue": "85.3"
      }
    ]
  }
}
```

---

#### 2.2 Core Metrics

**API Call:**

```bash
curl -s "https://sonarcloud.io/api/measures/component?component=hivemq_hivemq-edge&pullRequest=${PR_NUMBER}&metricKeys=alert_status,bugs,new_bugs,vulnerabilities,new_vulnerabilities,code_smells,new_code_smells,coverage,new_coverage,duplicated_lines_density,new_duplicated_lines_density,security_hotspots,new_security_hotspots,sqale_rating,new_maintainability_rating,reliability_rating,new_reliability_rating,security_rating,new_security_rating,ncloc,new_lines"
```

**Metrics to fetch:**

**Quality Gate:**

- `alert_status` - Overall gate status

**Bugs:**

- `bugs` - Total bugs (overall code)
- `new_bugs` - Bugs in new code (PR changes)

**Vulnerabilities:**

- `vulnerabilities` - Total vulnerabilities
- `new_vulnerabilities` - Vulnerabilities in new code

**Code Smells:**

- `code_smells` - Total code smells
- `new_code_smells` - Code smells in new code

**Coverage:**

- `coverage` - Overall test coverage %
- `new_coverage` - Test coverage % for new code

**Duplications:**

- `duplicated_lines_density` - % of duplicated lines (overall)
- `new_duplicated_lines_density` - % duplications in new code

**Security Hotspots:**

- `security_hotspots` - Total security hotspots
- `new_security_hotspots` - Security hotspots in new code

**Ratings (A-E scale):**

- `sqale_rating` / `new_maintainability_rating` - Maintainability
- `reliability_rating` / `new_reliability_rating` - Reliability
- `security_rating` / `new_security_rating` - Security

**Lines of Code:**

- `ncloc` - Total lines of code
- `new_lines` - New lines added in PR

---

#### 2.3 Issues Breakdown (if --detailed)

**API Call:**

```bash
curl -s "https://sonarcloud.io/api/issues/search?componentKeys=hivemq_hivemq-edge&pullRequest=${PR_NUMBER}&statuses=OPEN,CONFIRMED,REOPENED&ps=100"
```

**Group by:**

- Severity: `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `INFO`
- Type: `BUG`, `VULNERABILITY`, `CODE_SMELL`, `SECURITY_HOTSPOT`
- File: Group issues by file path

---

### Phase 3: Generate Report

Generate a formatted markdown report with the following structure:

````markdown
# SonarQube Analysis: PR #{pr_number}

**Project:** hivemq_hivemq-edge
**PR:** #{pr_number} - {pr_title}
**Analysis Date:** {timestamp}
**URL:** https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge&pullRequest={pr_number}

---

## Quality Gate: {PASSED / FAILED}

{emoji} **{status_message}**

{If failed, list failing conditions}

---

## New Code Metrics (PR Changes)

| Metric               | Value                           | Status     | Threshold |
| -------------------- | ------------------------------- | ---------- | --------- |
| 🐛 Bugs              | {new_bugs}                      | {✅/❌}    | 0         |
| 🔒 Vulnerabilities   | {new_vulnerabilities}           | {✅/❌}    | 0         |
| 💡 Code Smells       | {new_code_smells}               | {✅/⚠️/❌} | < 10      |
| 📊 Coverage          | {new_coverage}%                 | {✅/⚠️/❌} | > 80%     |
| 📋 Duplications      | {new_duplicated_lines_density}% | {✅/⚠️/❌} | < 3%      |
| 🔐 Security Hotspots | {new_security_hotspots}         | {✅/⚠️/❌} | 0         |

---

## Overall Project Metrics

| Metric                   | Value             | Rating   | Trend          |
| ------------------------ | ----------------- | -------- | -------------- |
| 🐛 Total Bugs            | {bugs}            | {rating} | {↑/↓/→}        |
| 🔒 Total Vulnerabilities | {vulnerabilities} | {rating} | {↑/↓/→}        |
| 💡 Total Code Smells     | {code_smells}     | {rating} | {↑/↓/→}        |
| 📊 Overall Coverage      | {coverage}%       | {rating} | {↑/↓/→}        |
| 📏 Lines of Code         | {ncloc}           | -        | (+{new_lines}) |

---

## Ratings

- **Maintainability:** {maintainability_rating} ({A-E})
- **Reliability:** {reliability_rating} ({A-E})
- **Security:** {security_rating} ({A-E})

---

## {If --detailed} Issues Breakdown

### 🔴 Blocker Issues ({count})

{List of blocker issues with file:line}

### 🟠 Critical Issues ({count})

{List of critical issues}

### 🟡 Major Issues ({count})

{List of major issues}

---

## Recommendations

{If bugs > 0}

- ⚠️ **Fix {new_bugs} new bugs** before merging

{If coverage < 80%}

- ⚠️ **Improve test coverage** from {new_coverage}% to at least 80%

{If code_smells > 10}

- 💡 **Address {new_code_smells} code smells** to improve maintainability

{If quality_gate == "FAILED"}

- 🔴 **Quality gate failed** - Address issues above before merging

{If quality_gate == "PASSED"}

- ✅ **Quality gate passed** - Code meets quality standards

---

## Quick Actions

**View full analysis:**
https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge&pullRequest={pr_number}

**View issues:**
https://sonarcloud.io/project/issues?id=hivemq_hivemq-edge&pullRequest={pr_number}

**Re-run analysis:**
\```bash
gh pr checks {pr_number} --rerun
\```
````

---

## Configuration

Configuration is stored in `.claude/skills/sonarqube/config.yaml`:

```yaml
sonarcloud:
  # Base URL for SonarCloud API
  base_url: https://sonarcloud.io

  # Organization key
  organization: hivemq

  # Project key
  project_key: hivemq_hivemq-edge

  # API version
  api_version: v1

github:
  # GitHub repository (owner/repo)
  repository: hivemq/hivemq-edge

  # Workflow names to check for SonarQube analysis
  workflow_names:
    - 'SonarQube Analysis'
    - 'SonarCloud Scan'
    - 'Code Quality'
    - 'CI' # Fallback if SonarQube runs in main CI

quality_gate:
  # Thresholds for metric interpretation
  thresholds:
    bugs: 0
    vulnerabilities: 0
    code_smells_warning: 5
    code_smells_error: 10
    coverage_warning: 70
    coverage_error: 80
    duplications_warning: 3
    duplications_error: 5

  # Rating interpretation (A=1, B=2, C=3, D=4, E=5)
  rating_pass: 2 # A or B acceptable

reporting:
  # Include detailed issues in report
  detailed_by_default: false

  # Max issues to show in report (per severity)
  max_issues_per_severity: 10

  # Show historical trends (requires multiple analyses)
  show_trends: true

  # Emoji mapping for statuses
  emoji:
    passed: '✅'
    failed: '❌'
    warning: '⚠️'
    info: 'ℹ️'
    bug: '🐛'
    vulnerability: '🔒'
    code_smell: '💡'
    coverage: '📊'
    duplications: '📋'
    security_hotspot: '🔐'

# Metadata
version: '1.0.0'
last_updated: '2026-02-10'
project: 'hivemq-edge-frontend'
```

---

## Error Handling

### Error 1: No PR Found (Hook 1 Failure)

**Message:**

```
❌ No pull request found for current branch: {branch_name}

**Next steps:**
1. Create a PR first:
   gh pr create --title "Your PR title" --body "Description"

2. Or specify a PR number:
   /sonarqube 1386

3. Or force skip PR check:
   /sonarqube --force
```

---

### Error 2: GH Action Not Completed (Hook 2 Failure)

**Message:**

```
⏳ SonarQube analysis is still running...

**Status:** IN_PROGRESS
**Started:** 2 minutes ago
**Estimated completion:** ~3 minutes

**Options:**
1. Wait for completion (will auto-check every 30s)
2. Continue anyway with potentially stale data: /sonarqube --force
3. View action logs: https://github.com/hivemq/hivemq-edge/actions/runs/12345
```

---

### Error 3: SonarQube Data Not Available (Hook 3 Failure)

**Message:**

```
❌ SonarQube has no analysis data for PR #1386

**Possible causes:**
1. Analysis hasn't run yet (GH action failed?)
2. PR is too new (analysis in progress)
3. SonarQube configuration issue

**Next steps:**
1. Check GH action status: gh pr checks 1386
2. Re-run GH workflow: gh pr checks 1386 --rerun
3. Wait a few minutes and try again
4. Check SonarQube project settings
```

---

### Error 4: API Rate Limit

**Message:**

```
⚠️ SonarCloud API rate limit reached

**Options:**
1. Wait 60 seconds and retry
2. Use web interface: https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge&pullRequest={pr_number}
```

---

## Integration with Other Skills

### With /pre-review Skill

**Workflow:**

```bash
# 1. Run pre-review first (local checks)
/pre-review

# 2. Fix critical issues

# 3. Create PR
gh pr create

# 4. Wait for CI to complete

# 5. Run SonarQube analysis
/sonarqube

# 6. Address SonarQube findings

# 7. Ready to merge!
```

**Combined report:**

- Pre-review catches local pattern violations
- SonarQube validates against broader quality metrics
- Both reports reference each other

---

## Advanced Usage

### Compare Multiple PRs

```bash
# Analyze multiple PRs
/sonarqube 1386
/sonarqube 1387

# Compare metrics manually
```

### Branch Analysis (Without PR)

```bash
# Analyze a branch directly
/sonarqube feature/new-feature
```

### Historical Trends

```bash
# Show how metrics changed over time
/sonarqube 1386 --trends
```

---

## Testing the Skill

**Test cases:**

1. **Happy path:**

   ```bash
   /sonarqube  # Should auto-detect PR and fetch metrics
   ```

2. **No PR:**

   ```bash
   # On a branch without PR
   /sonarqube  # Should prompt to create PR or use --force
   ```

3. **GH action in progress:**

   ```bash
   /sonarqube  # Should wait or show progress
   ```

4. **Failed quality gate:**

   ```bash
   /sonarqube <pr_with_issues>  # Should show failing conditions
   ```

5. **Detailed report:**
   ```bash
   /sonarqube --detailed  # Should show issue breakdown
   ```

---

## Future Enhancements

**Planned features:**

- [ ] Auto-comment on PR with quality metrics
- [ ] Trend analysis (compare with previous PRs)
- [ ] Blocker detection (prevent merge if critical issues)
- [ ] Integration with GitHub Status Checks
- [ ] Custom quality gate definitions
- [ ] Export metrics to CSV/JSON
- [ ] Slack/Teams notification integration
- [ ] Historical metrics visualization
- [ ] AI-powered issue prioritization

---

## Hooks System (For Skill Developers)

### Hook Definition

Hooks are **prerequisite checks** that must pass before the main skill logic runs.

**Structure:**

```yaml
hooks:
  - id: hook-1
    name: 'Descriptive Name'
    objective: 'What this hook validates'
    commands: ['bash command to check']
    success_criteria: 'What indicates success'
    failure_handling: 'What to do on failure'
    can_skip: true/false
    skip_flag: '--force'
```

**Hook execution:**

1. Run in order (Hook 1 → Hook 2 → Hook 3)
2. If any hook fails and `can_skip: false`, abort skill
3. If `can_skip: true`, prompt user with options
4. If `skip_flag` provided, skip all skippable hooks

**Example in skill:**

````markdown
### Hook 1: PR Existence Check

**Commands:**

```bash
gh pr view --json number,title,state
```
````

**Success criteria:**

- Exit code 0
- JSON contains `"state": "OPEN"`

**Failure handling:**

- Prompt user to create PR
- Or ask for manual PR number
- Or skip with --force

```

---

**Generated:** 2026-02-10
**Version:** 1.0.0
**Status:** Ready for use
```
