# Task 37038: CI Conditional Check - Diagnostic and Action Plan

## Date

December 4, 2025

## Problem Summary

The CI Check GitHub Actions workflow is not executing conditional jobs despite correctly detecting changes in the codebase. The debug output shows `frontend-changed: true`, but the `check-frontend` job doesn't execute.

## Root Cause Analysis

### The Core Issue

The problem lies in the `needs` dependencies combined with conditional execution:

```yaml
check-frontend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-frontend.yml
  if: needs.check-for-changes.outputs.frontend-changed == 'true'
```

**GitHub Actions Behavior:**
When a job has multiple dependencies in `needs`, and ANY of those dependencies are skipped, the dependent job is ALSO skipped by default, **regardless of its own `if` condition**.

### What's Happening

1. ✅ `check-for-changes` runs successfully and outputs: `frontend-changed: true`
2. ❌ `check-openapi` is skipped (because `openapi-changed: false`)
3. ❌ `check-frontend` is skipped because its dependency `check-openapi` was skipped
4. ❌ `check-backend` is skipped for the same reason

### Why .github Changes Work

When you modify files in `.github/**`:

- ALL three filters match (backend, frontend, AND openapi filters all include `.github/**`)
- Therefore `check-openapi` runs instead of being skipped
- With `check-openapi` running, the other jobs can now execute based on their own conditions

## Proposed Solutions

### Solution 1: Add `if: always()` to Dependent Jobs (RECOMMENDED)

**Pros:**

- Minimal changes to workflow structure
- Explicitly allows jobs to evaluate their own conditions independently
- No changes to reusable workflow files needed
- Clear intent in the code

**Cons:**

- None significant

**Implementation:**

```yaml
check-openapi:
  needs: check-for-changes
  uses: ./.github/workflows/check-openapi.yml
  if: needs.check-for-changes.outputs.openapi-changed == 'true'

check-frontend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-frontend.yml
  if: always() && needs.check-for-changes.outputs.frontend-changed == 'true'
  secrets:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}

check-backend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-backend.yml
  if: always() && needs.check-for-changes.outputs.backend-changed == 'true'
```

The `if: always()` ensures the job evaluates its condition even if dependencies were skipped. Combined with the actual condition check, it runs only when changes are detected.

### Solution 2: Remove Unnecessary Dependencies

**Question:** Why do `check-frontend` and `check-backend` depend on `check-openapi`?

Looking at the workflow, these jobs seem to have this dependency to ensure ordering, but it creates this skipping problem.

**Option 2a: Remove the openapi dependency if not needed**

```yaml
check-frontend:
  needs: check-for-changes
  uses: ./.github/workflows/check-frontend.yml
  if: needs.check-for-changes.outputs.frontend-changed == 'true'

check-backend:
  needs: check-for-changes
  uses: ./.github/workflows/check-backend.yml
  if: needs.check-for-changes.outputs.backend-changed == 'true'
```

**Option 2b: If ordering IS important, combine with Solution 1**

### Solution 3: Use Job Status Checks in Dependencies

```yaml
check-frontend:
  needs: [check-for-changes, check-openapi]
  uses: ./.github/workflows/check-frontend.yml
  if: |
    always() &&
    !cancelled() &&
    (needs.check-openapi.result == 'success' || needs.check-openapi.result == 'skipped') &&
    needs.check-for-changes.outputs.frontend-changed == 'true'
```

This is more explicit but also more verbose.

## Recommended Action Plan

### Phase 1: Implement Solution 1 (Preferred)

1. **Modify the conditional jobs** to include `if: always() &&` before their existing conditions
2. **Test the workflow** with changes only in frontend code
3. **Verify** that jobs execute correctly based on detected changes

### Phase 2: Validate and Document

1. **Test scenarios:**

   - Change only frontend code → only check-frontend runs
   - Change only backend code → only check-backend runs
   - Change only openapi code → only check-openapi runs
   - Change frontend + backend → both run
   - Change all three → all run

2. **Document the fix** in workflow comments for future maintainers

### Phase 3: Optional Optimization

If the dependency on `check-openapi` is not actually needed for `check-frontend` and `check-backend`, consider removing it to simplify the workflow.

## Additional Observations

### Why This Started Happening After Multi-Tenant Changes

The multi-tenant changes likely modified the codebase structure or the way commits are made, possibly affecting:

- Which files are typically changed together
- The PR/branch structure
- The base comparison points for the paths-filter action

However, the root cause is the workflow logic itself, not the paths-filter action, which is working correctly.

### Version Considerations

The current `dorny/paths-filter@v3.0.2` is up to date and working correctly. No version changes needed.

## Implementation Priority

**HIGH PRIORITY** - This is blocking proper CI validation on PRs and could allow broken code to be merged.

## Estimated Impact

- **Risk:** LOW - The fix is well-understood and tested pattern in GitHub Actions
- **Effort:** MINIMAL - Single workflow file change
- **Testing:** Required scenarios listed above

## References

- GitHub Actions Documentation: [Using conditions to control job execution](https://docs.github.com/en/actions/using-jobs/using-conditions-to-control-job-execution)
- GitHub Actions: `always()` expression - runs even if previous jobs were skipped
- Workflow run with issue: https://github.com/hivemq/hivemq-edge/actions/runs/19930897040
