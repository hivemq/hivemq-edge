# Implementation Summary - Task 37038: CI Conditional Check

## Date

December 4, 2025

## Changes Made

### File Modified

- `.github/workflows/check.yml`

### Changes Applied

#### 1. Updated Job Conditions (Lines ~79-91)

**Before:**

```yaml
check-frontend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-frontend.yml
  if: needs.check-for-changes.outputs.frontend-changed == 'true'

check-backend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-backend.yml
  if: needs.check-for-changes.outputs.backend-changed == 'true'
```

**After:**

```yaml
check-frontend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-frontend.yml
  # Always evaluate condition even if check-openapi was skipped
  if: always() && needs.check-for-changes.outputs.frontend-changed == 'true'

check-backend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-backend.yml
  # Always evaluate condition even if check-openapi was skipped
  if: always() && needs.check-for-changes.outputs.backend-changed == 'true'
```

#### 2. Added Documentation Comment (Line ~21)

Added explanatory comment at the top of the `jobs` section:

```yaml
jobs:
  # Note: check-frontend and check-backend use `if: always() &&` to ensure they
  # evaluate their conditions even when check-openapi is skipped.
  # Without this, GitHub Actions would skip them when any dependency is skipped.
```

## What This Fixes

### The Problem

- When `check-openapi` was skipped (no openapi changes), dependent jobs were also skipped
- This happened despite the change detection working correctly
- The `if: always() &&` modifier allows jobs to evaluate their own conditions independently

### Expected Behavior After Fix

| Scenario              | check-openapi | check-frontend | check-backend |
| --------------------- | ------------- | -------------- | ------------- |
| Frontend only changes | Skipped       | ✅ Runs        | Skipped       |
| Backend only changes  | Skipped       | Skipped        | ✅ Runs       |
| OpenAPI only changes  | ✅ Runs       | Skipped        | Skipped       |
| Frontend + Backend    | Skipped       | ✅ Runs        | ✅ Runs       |
| All changes           | ✅ Runs       | ✅ Runs        | ✅ Runs       |
| .github changes       | ✅ Runs       | ✅ Runs        | ✅ Runs       |

## Testing Plan

### Test Scenarios

1. **Frontend-only changes** (This PR)

   - Modify files in `hivemq-edge-frontend/`
   - Expected: `check-frontend` should run, others skip
   - This is the immediate test case

2. **Backend-only changes**

   - Modify files in `hivemq-edge/`
   - Expected: `check-backend` should run, others skip

3. **OpenAPI-only changes**

   - Modify files in `hivemq-edge-openapi/`
   - Expected: `check-openapi` should run, others skip

4. **Multiple changes**
   - Modify files in multiple directories
   - Expected: Only relevant checks should run

### Validation Checklist

- [ ] Workflow file syntax is valid (GitHub will validate on push)
- [ ] Jobs run when their respective changes are detected
- [ ] Jobs are skipped when no relevant changes are detected
- [ ] The final `check` job correctly validates all results
- [ ] No regression in existing functionality

## Rollback Plan

If issues occur, revert by removing `always() &&` from the two modified `if` conditions:

```yaml
# Revert to:
if: needs.check-for-changes.outputs.frontend-changed == 'true'
if: needs.check-for-changes.outputs.backend-changed == 'true'
```

## Impact Assessment

- **Risk Level:** LOW

  - Well-understood GitHub Actions pattern
  - Minimal change to workflow logic
  - Easy to revert if needed

- **Affected Systems:**

  - CI/CD pipeline
  - PR validation workflow
  - No impact on deployed applications

- **Dependencies:**
  - No changes to reusable workflow files
  - No changes to action versions
  - No external dependency updates

## References

- Original issue: https://github.com/hivemq/hivemq-edge/actions/runs/19930897040
- GitHub Actions docs: [Expressions - Status check functions](https://docs.github.com/en/actions/learn-github-actions/expressions#status-check-functions)
- Task directory: `.tasks/37038-ci-conditional-check/`

## Next Steps

1. ✅ Commit and push changes
2. ⏳ Monitor workflow execution on this branch
3. ⏳ Verify all test scenarios
4. ⏳ Create PR if needed or merge directly if approved
5. ⏳ Close task 37038 after validation
