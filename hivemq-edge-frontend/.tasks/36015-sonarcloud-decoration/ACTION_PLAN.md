# SonarCloud Decoration - Action Plan

**Task:** 36015-sonarcloud-decoration  
**Date:** October 24, 2025

## Executive Summary

The HiveMQ Edge Frontend has a SonarCloud configuration and GitHub Actions workflow in place, but it's using the **wrong GitHub Action** (SonarQube instead of SonarCloud) which prevents proper PR decoration. This document outlines the issues and proposed fixes.

## Critical Issues Found

### 🔴 Issue #1: Wrong GitHub Action (BLOCKING PR DECORATION)

**Current State:**
```yaml
- name: SonarQube Scan
  uses: SonarSource/sonarqube-scan-action@2500896589ef8f7247069a56136f8dc177c27ccf # v5
```

**Problem:**
- This action is for **SonarQube** (self-hosted server)
- SonarQube action **cannot** decorate GitHub PRs with SonarCloud
- Missing required PR decoration parameters

**Impact:** PR decoration is not working at all

**Fix Required:**
```yaml
- name: SonarCloud Scan
  uses: SonarSource/sonarcloud-github-action@v3.0.0
```

---

### 🟡 Issue #2: Missing PR Context

**Current State:**
- No pull request number passed to SonarCloud
- No base branch configuration
- Missing PR-specific metadata

**Impact:** Even if we switch actions, PR decoration won't have context

**Fix Required:**
Add GitHub PR context to the scan:
```yaml
env:
  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

The `GITHUB_TOKEN` enables automatic PR decoration.

---

### 🟡 Issue #3: Coverage Path Configuration

**Current Setup:**
- Artifacts uploaded as separate folders: `lcov-cypress-Components`, `lcov-cypress-E2E`, etc.
- Downloaded to: `hivemq-edge-frontend/coverage-combined/`
- Config expects: `./coverage-combined/lcov-*/lcov.info`

**Potential Issue:**
Path resolution depends on working directory context. Currently using:
```yaml
with:
  projectBaseDir: hivemq-edge-frontend
```

**Status:** Likely working, but worth verifying in logs

---

### 🟢 Issue #4: Job Execution Condition

**Current State:**
```yaml
if: success() || failure()
```

**Analysis:**
- Runs even if tests fail
- Submits partial/incomplete coverage
- May be intentional for catching quality issues

**Recommendation:** Keep current behavior, but document why

---

## Configuration Review

### ✅ Well-Configured Items

1. **Project Identification:**
   ```ini
   sonar.projectKey=hivemq_hivemq-edge
   sonar.organization=hivemq
   ```

2. **Coverage Sources:**
   - 6 LCOV reports (5 Cypress splits + Vitest)
   - Proper path structure

3. **Exclusions:**
   - Generated files
   - Test utilities
   - Schema files
   - Tools directory

4. **Test Inclusions:**
   - All test file patterns covered

---

## Proposed Changes

### Change #1: Switch to SonarCloud Action

**File:** `.github/workflows/check-frontend.yml`

**Before:**
```yaml
- name: SonarQube Scan
  uses: SonarSource/sonarqube-scan-action@2500896589ef8f7247069a56136f8dc177c27ccf # v5
  with:
    projectBaseDir: hivemq-edge-frontend
    args: >
      -Dsonar.verbose=false
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

**After:**
```yaml
- name: SonarCloud Scan
  uses: SonarSource/sonarcloud-github-action@v3.0.0
  with:
    projectBaseDir: hivemq-edge-frontend
    args: >
      -Dsonar.verbose=false
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

**Key Changes:**
1. Switch from `sonarqube-scan-action` → `sonarcloud-github-action`
2. Add `GITHUB_TOKEN` for PR decoration
3. Update job name from "SonarQube" → "SonarCloud"

---

### Change #2: Add Quality Gate Check (Optional Enhancement)

Add a quality gate status check after the scan:

```yaml
- name: SonarCloud Quality Gate
  uses: SonarSource/sonarqube-quality-gate-action@v1.1.0
  timeout-minutes: 5
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

This will fail the workflow if quality gate doesn't pass.

---

### Change #3: Improve Artifact Verification

**Current:**
```yaml
- name: Verify LCOV Artifacts
  run: |
    ls -R ./coverage-combined
    ls -R **/**/*.info
```

**Enhanced:**
```yaml
- name: Verify LCOV Artifacts
  run: |
    echo "=== Coverage artifacts downloaded ==="
    ls -la ./coverage-combined/
    echo ""
    echo "=== LCOV files found ==="
    find ./coverage-combined -name "lcov.info" -type f
    echo ""
    echo "=== File sizes ==="
    find ./coverage-combined -name "lcov.info" -type f -exec ls -lh {} \;
```

This provides better debugging information.

---

## Implementation Plan

### Phase 1: Critical Fix (REQUIRED)
1. ✅ Audit current configuration (COMPLETED)
2. 🔲 Switch to `sonarcloud-github-action`
3. 🔲 Add `GITHUB_TOKEN` environment variable
4. 🔲 Test on a PR

### Phase 2: Verification (REQUIRED)
1. 🔲 Verify PR decoration appears on pull requests
2. 🔲 Verify coverage reports are uploaded correctly
3. 🔲 Check SonarCloud dashboard for analysis results

### Phase 3: Enhancements (OPTIONAL)
1. 🔲 Add quality gate check action
2. 🔲 Improve artifact verification logging
3. 🔲 Document the setup for team members

---

## Testing Strategy

### Test 1: Create Test PR
1. Make a trivial change to a frontend file
2. Create a PR
3. Verify the workflow runs
4. Check for SonarCloud decoration on the PR

### Test 2: Verify Coverage
1. Check SonarCloud dashboard
2. Verify all 6 coverage reports are processed
3. Confirm coverage metrics are accurate

### Test 3: Quality Gate
1. Introduce a deliberate code smell
2. Verify SonarCloud detects it
3. Verify it appears in PR decoration

---

## Risk Assessment

### Low Risk
- Switching GitHub Actions (both are official SonarSource actions)
- Adding GITHUB_TOKEN (standard practice)

### Medium Risk
- Coverage path resolution (already configured, unlikely to break)

### High Risk
- None identified

---

## Rollback Plan

If issues occur:
1. Revert the workflow file change
2. Original SHA: `2500896589ef8f7247069a56136f8dc177c27ccf`
3. Workflow will continue to run (just without PR decoration)

---

## Success Criteria

- ✅ SonarCloud analysis runs on every PR
- ✅ PR shows SonarCloud decoration with:
  - Code quality metrics
  - Coverage changes
  - New issues found
  - Quality gate status
- ✅ Team can see analysis results before merging
- ✅ No workflow failures due to configuration issues

---

## References

- SonarCloud GitHub Action: https://github.com/SonarSource/sonarcloud-github-action
- SonarCloud PR Decoration: https://docs.sonarcloud.io/enriching/pr-decoration/
- Current workflow: `.github/workflows/check-frontend.yml`
- Configuration: `hivemq-edge-frontend/sonar-project.properties`

