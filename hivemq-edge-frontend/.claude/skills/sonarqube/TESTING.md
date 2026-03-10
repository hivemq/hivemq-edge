# Testing the SonarQube Skill

This document provides test cases and example API responses for testing the skill.

## Test Cases

### Test 1: Happy Path (All Hooks Pass)

**Setup:**

- Current branch has an open PR
- GitHub Action has completed successfully
- SonarQube has analyzed the PR

**Command:**

```bash
/sonarqube
```

**Expected Behavior:**

1. Hook 1: ✅ Finds PR #1386
2. Hook 2: ✅ GH Action completed (success)
3. Hook 3: ✅ Fetches SonarQube data
4. Generates full report with all metrics

---

### Test 2: No PR Exists (Hook 1 Fails)

**Setup:**

- Current branch has no PR

**Command:**

```bash
/sonarqube
```

**Expected Behavior:**

1. Hook 1: ❌ No PR found
2. Shows error message with options:
   - Create PR: `gh pr create`
   - Specify PR: `/sonarqube 1386`
   - Force skip: `/sonarqube --force`
3. Does NOT proceed to Hook 2

---

### Test 3: GH Action In Progress (Hook 2 Waits)

**Setup:**

- PR exists
- GH Action is currently running

**Command:**

```bash
/sonarqube
```

**Expected Behavior:**

1. Hook 1: ✅ Finds PR
2. Hook 2: ⏳ Action in progress
3. Shows status: "Running for 2 minutes, ~3 minutes remaining"
4. Options:
   - Wait (auto-poll every 30s)
   - Force skip: `/sonarqube --force`

---

### Test 4: GH Action Failed (Hook 2 Fails)

**Setup:**

- PR exists
- GH Action failed

**Command:**

```bash
/sonarqube
```

**Expected Behavior:**

1. Hook 1: ✅ Finds PR
2. Hook 2: ❌ Action failed
3. Shows failure message with logs URL
4. Options:
   - Fix and re-run: `gh pr checks <pr> --rerun`
   - Continue with stale data: `/sonarqube --force`

---

### Test 5: No SonarQube Data (Hook 3 Fails)

**Setup:**

- PR exists
- GH Action passed
- SonarQube has no data for this PR

**Command:**

```bash
/sonarqube
```

**Expected Behavior:**

1. Hook 1: ✅ Finds PR
2. Hook 2: ✅ GH Action passed
3. Hook 3: ❌ No SonarQube data
4. Shows error: "SonarQube hasn't analyzed this PR yet"
5. Suggests re-running GH workflow

---

### Test 6: Quality Gate Failed

**Setup:**

- All hooks pass
- Quality gate status is "FAILED"

**Command:**

```bash
/sonarqube
```

**Expected Behavior:**

1. All hooks pass
2. Report shows: ❌ Quality Gate: FAILED
3. Lists failing conditions:
   - Coverage: 65% (threshold: 80%)
   - New bugs: 3 (threshold: 0)
4. Recommendations section highlights issues

---

### Test 7: Detailed Report

**Setup:**

- All hooks pass
- PR has some code smells

**Command:**

```bash
/sonarqube --detailed
```

**Expected Behavior:**

1. All hooks pass
2. Report includes "Issues Breakdown" section
3. Lists up to 10 issues per severity:
   - 🔴 Blocker: 0
   - 🟠 Critical: 0
   - 🟡 Major: 3 (with file:line references)
   - 🔵 Minor: 5
4. Shows file-level metrics

---

### Test 8: Quality Gate Only (Quick Check)

**Setup:**

- All hooks pass

**Command:**

```bash
/sonarqube --quality-gate-only
```

**Expected Behavior:**

1. All hooks pass
2. Minimal report showing only:
   - Quality Gate: PASSED/FAILED
   - Overall status emoji
   - Link to full analysis
3. Skips detailed metrics

---

### Test 9: Force Skip All Hooks

**Setup:**

- Branch has no PR
- Want to analyze anyway

**Command:**

```bash
/sonarqube feature/new-feature --force
```

**Expected Behavior:**

1. Skips Hook 1 and Hook 2
2. Goes directly to Hook 3 (SonarQube API)
3. Attempts to fetch data for branch "feature/new-feature"
4. Generates report if data exists

---

### Test 10: Specific PR Number

**Setup:**

- Current branch is different
- Want to analyze PR #1386

**Command:**

```bash
/sonarqube 1386
```

**Expected Behavior:**

1. Skips auto-detection
2. Uses PR #1386 for all hooks
3. Normal flow for that PR

---

## Example API Responses

### Example 1: Quality Gate Status (PASSED)

**API Call:**

```bash
curl "https://sonarcloud.io/api/qualitygates/project_status?projectKey=hivemq_hivemq-edge&pullRequest=1386"
```

**Response:**

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
      },
      {
        "status": "OK",
        "metricKey": "new_duplicated_lines_density",
        "comparator": "GT",
        "errorThreshold": "3",
        "actualValue": "1.2"
      },
      {
        "status": "OK",
        "metricKey": "new_bugs",
        "comparator": "GT",
        "errorThreshold": "0",
        "actualValue": "0"
      }
    ],
    "periods": [
      {
        "index": 1,
        "mode": "previous_version",
        "date": "2026-02-09T10:00:00+0000"
      }
    ],
    "ignoredConditions": false
  }
}
```

---

### Example 2: Quality Gate Status (FAILED)

**Response:**

```json
{
  "projectStatus": {
    "status": "ERROR",
    "conditions": [
      {
        "status": "ERROR",
        "metricKey": "new_coverage",
        "comparator": "LT",
        "errorThreshold": "80",
        "actualValue": "65.5"
      },
      {
        "status": "ERROR",
        "metricKey": "new_bugs",
        "comparator": "GT",
        "errorThreshold": "0",
        "actualValue": "3"
      },
      {
        "status": "OK",
        "metricKey": "new_duplicated_lines_density",
        "comparator": "GT",
        "errorThreshold": "3",
        "actualValue": "1.2"
      }
    ]
  }
}
```

**Interpretation:**

- Coverage: FAILED (65.5% < 80%)
- Bugs: FAILED (3 bugs > 0)
- Duplications: PASSED (1.2% < 3%)

---

### Example 3: Component Measures

**API Call:**

```bash
curl "https://sonarcloud.io/api/measures/component?component=hivemq_hivemq-edge&pullRequest=1386&metricKeys=alert_status,bugs,new_bugs,vulnerabilities,new_vulnerabilities,code_smells,new_code_smells,coverage,new_coverage"
```

**Response:**

```json
{
  "component": {
    "key": "hivemq_hivemq-edge",
    "name": "HiveMQ Edge",
    "qualifier": "TRK",
    "measures": [
      {
        "metric": "alert_status",
        "value": "OK"
      },
      {
        "metric": "bugs",
        "value": "12"
      },
      {
        "metric": "new_bugs",
        "value": "0"
      },
      {
        "metric": "vulnerabilities",
        "value": "3"
      },
      {
        "metric": "new_vulnerabilities",
        "value": "0"
      },
      {
        "metric": "code_smells",
        "value": "245"
      },
      {
        "metric": "new_code_smells",
        "value": "2"
      },
      {
        "metric": "coverage",
        "value": "78.5"
      },
      {
        "metric": "new_coverage",
        "value": "85.3"
      }
    ]
  }
}
```

---

### Example 4: Issues Search (Detailed Mode)

**API Call:**

```bash
curl "https://sonarcloud.io/api/issues/search?componentKeys=hivemq_hivemq-edge&pullRequest=1386&statuses=OPEN&ps=100"
```

**Response:**

```json
{
  "total": 5,
  "p": 1,
  "ps": 100,
  "issues": [
    {
      "key": "AYzF1X2Y3Z4A5B6C7D8E",
      "rule": "typescript:S1186",
      "severity": "MAJOR",
      "component": "hivemq_hivemq-edge:src/modules/Mappings/combiner/CombinedEntitySelect.tsx",
      "project": "hivemq_hivemq-edge",
      "line": 145,
      "message": "Add a nested comment explaining why this function is empty or complete the implementation.",
      "type": "CODE_SMELL"
    },
    {
      "key": "AYzF1X2Y3Z4A5B6C7D8F",
      "rule": "typescript:S125",
      "severity": "MINOR",
      "component": "hivemq_hivemq-edge:src/modules/Mappings/utils/combining.utils.ts",
      "project": "hivemq_hivemq-edge",
      "line": 223,
      "message": "Remove this commented out code.",
      "type": "CODE_SMELL"
    }
  ]
}
```

---

### Example 5: GitHub PR Check Status

**API Call:**

```bash
gh pr checks 1386 --json name,conclusion,status,detailsUrl
```

**Response:**

```json
[
  {
    "name": "CI",
    "conclusion": "SUCCESS",
    "status": "COMPLETED",
    "detailsUrl": "https://github.com/hivemq/hivemq-edge/actions/runs/12345"
  },
  {
    "name": "SonarCloud Scan",
    "conclusion": "SUCCESS",
    "status": "COMPLETED",
    "detailsUrl": "https://github.com/hivemq/hivemq-edge/actions/runs/12346"
  },
  {
    "name": "Tests",
    "conclusion": "SUCCESS",
    "status": "COMPLETED",
    "detailsUrl": "https://github.com/hivemq/hivemq-edge/actions/runs/12347"
  }
]
```

---

### Example 6: GitHub PR Info

**API Call:**

```bash
gh pr view 1386 --json number,title,state,url,createdAt,updatedAt
```

**Response:**

```json
{
  "number": 1386,
  "title": "feat: Frontend Ownership Tracking for Data Combiners",
  "state": "OPEN",
  "url": "https://github.com/hivemq/hivemq-edge/pull/1386",
  "createdAt": "2026-02-09T14:23:45Z",
  "updatedAt": "2026-02-10T11:45:12Z"
}
```

---

## Manual Testing Commands

### Test Hook 1 (PR Existence)

```bash
# Should return PR info
gh pr view --json number,title,state,url

# Should return exit code 0 if PR exists
echo $?
```

---

### Test Hook 2 (GH Action Status)

```bash
# Get all checks for PR
gh pr checks 1386 --json name,conclusion,status,detailsUrl

# Filter for SonarQube-related
gh pr checks 1386 --json name,conclusion,status | \
  jq '.[] | select(.name | contains("SonarQube") or contains("sonarcloud"))'
```

---

### Test Hook 3 (SonarQube API)

```bash
# Test quality gate endpoint
curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=hivemq_hivemq-edge&pullRequest=1386" | jq .

# Test measures endpoint
curl -s "https://sonarcloud.io/api/measures/component?component=hivemq_hivemq-edge&pullRequest=1386&metricKeys=alert_status,bugs,coverage" | jq .

# Test issues endpoint
curl -s "https://sonarcloud.io/api/issues/search?componentKeys=hivemq_hivemq-edge&pullRequest=1386&ps=10" | jq .
```

---

## Expected Report Examples

### Example Report: All Green

```markdown
# SonarQube Analysis: PR #1386

**Project:** hivemq_hivemq-edge
**PR:** #1386 - feat: Frontend Ownership Tracking for Data Combiners
**Analysis Date:** 2026-02-10 14:32 UTC
**URL:** https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge&pullRequest=1386

---

## Quality Gate: ✅ PASSED

All quality gate conditions met!

---

## New Code Metrics (PR Changes)

| Metric               | Value | Status | Threshold |
| -------------------- | ----- | ------ | --------- |
| 🐛 Bugs              | 0     | ✅     | 0         |
| 🔒 Vulnerabilities   | 0     | ✅     | 0         |
| 💡 Code Smells       | 2     | ✅     | < 10      |
| 📊 Coverage          | 85.3% | ✅     | > 80%     |
| 📋 Duplications      | 1.2%  | ✅     | < 3%      |
| 🔐 Security Hotspots | 0     | ✅     | 0         |

---

## Recommendations

✅ **Quality gate passed** - Code meets quality standards
💡 Consider addressing 2 code smells for improved maintainability
```

---

### Example Report: Failed Quality Gate

```markdown
# SonarQube Analysis: PR #1234

## Quality Gate: ❌ FAILED

The following conditions failed:

- ❌ **Coverage:** 65.5% (required: ≥80%)
- ❌ **New Bugs:** 3 (required: 0)

---

## New Code Metrics (PR Changes)

| Metric      | Value | Status | Threshold |
| ----------- | ----- | ------ | --------- |
| 🐛 Bugs     | 3     | ❌     | 0         |
| 📊 Coverage | 65.5% | ❌     | > 80%     |

---

## Recommendations

🔴 **Quality gate failed** - Must fix before merging

**Priority actions:**

1. ⚠️ **Fix 3 new bugs** - See details in full report
2. ⚠️ **Improve test coverage** from 65.5% to at least 80%
```

---

## Debugging

### Enable Debug Logging

Edit `config.yaml`:

```yaml
logging:
  level: debug
  log_api_calls: true
```

### Check Logs

```bash
cat .claude/skills/sonarqube/.logs/sonarqube.log
```

### Test Individual API Calls

```bash
# Save responses to files for inspection
curl "https://sonarcloud.io/api/qualitygates/project_status?projectKey=hivemq_hivemq-edge&pullRequest=1386" > quality_gate.json

curl "https://sonarcloud.io/api/measures/component?component=hivemq_hivemq-edge&pullRequest=1386&metricKeys=bugs,coverage" > measures.json
```

---

**Last Updated:** 2026-02-10
**Version:** 1.0.0
