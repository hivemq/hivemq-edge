# Testing Guide - CI Conditional Check Fix

## Quick Test: Verify the Fix Works

This branch already has frontend changes (the TASK_BRIEF.md file), so you can test immediately!

### Test 1: Current Branch (Frontend Changes)

**What to check:**

1. Push this branch with the workflow fix
2. Go to Actions tab: https://github.com/hivemq/hivemq-edge/actions
3. Find the workflow run for this branch

**Expected Results:**

```
✅ check-for-changes: Success
   Debug output should show: frontend-changed: true

⏭️ check-openapi: Skipped (no openapi changes)

✅ check-frontend: SHOULD RUN (this is the fix!)
   Previously this was skipped, now it should execute

⏭️ check-backend: Skipped (no backend changes)

✅ check: Success (validates all results)
```

### Test 2: Backend-Only Changes

**How to test:**

```bash
# Create a test file in backend
echo "# Test" > ../hivemq-edge/TEST_FILE.md
git add ../hivemq-edge/TEST_FILE.md
git commit -m "test: backend change detection"
git push
```

**Expected Results:**

- ✅ check-backend runs
- ⏭️ check-frontend skipped
- ⏭️ check-openapi skipped

### Test 3: Multiple Changes

**How to test:**

```bash
# Add changes to multiple areas
echo "# Test" >> hivemq-edge-frontend/README.md
echo "# Test" >> ../hivemq-edge/README.md
git add .
git commit -m "test: multiple changes"
git push
```

**Expected Results:**

- ✅ check-backend runs
- ✅ check-frontend runs
- ⏭️ check-openapi skipped

## Verification Commands

### Check workflow syntax locally (if gh CLI installed)

```bash
# This would validate the YAML syntax
gh workflow view check.yml --repo hivemq/hivemq-edge
```

### View workflow run details

```bash
# Replace RUN_ID with actual run ID
gh run view RUN_ID --repo hivemq/hivemq-edge
```

### Watch workflow run in real-time

```bash
# After pushing
gh run watch --repo hivemq/hivemq-edge
```

## What Success Looks Like

### Before the Fix

```
check-for-changes: ✅ (detects changes correctly)
  └─ Debug: "frontend-changed: true"

check-openapi: ⏭️ (skipped - no changes)

check-frontend: ⏭️ (WRONGLY SKIPPED - dependency was skipped)
                ⚠️ This is the bug!

check-backend: ⏭️ (skipped - no changes)

check: ✅ (validates but nothing ran)
```

### After the Fix

```
check-for-changes: ✅ (detects changes correctly)
  └─ Debug: "frontend-changed: true"

check-openapi: ⏭️ (skipped - no changes)

check-frontend: ✅ (NOW RUNS - evaluates its own condition)
                ✨ Fixed!

check-backend: ⏭️ (skipped - no changes)

check: ✅ (validates actual test results)
```

## Troubleshooting

### If check-frontend still doesn't run:

1. **Check the workflow file was actually updated:**

   ```bash
   git show HEAD:.github/workflows/check.yml | grep -A 2 "check-frontend:"
   ```

   Should show: `if: always() && needs.check-for-changes.outputs.frontend-changed == 'true'`

2. **Check if changes are in the right directory:**

   - Frontend changes must be in `hivemq-edge-frontend/**`
   - This task file location is correct: `hivemq-edge-frontend/.tasks/37038-ci-conditional-check/`

3. **Verify the paths-filter is working:**
   - Look at the "Debug changed output" step in check-for-changes job
   - Should show `frontend-changed: true`

### If jobs fail with errors:

- Check the error message in the workflow run
- Verify secrets are available (SONAR_TOKEN, PERCY_TOKEN)
- The workflow already has `continue-on-error: true` for known issues

## GitHub Actions UI Navigation

1. Go to repository: https://github.com/hivemq/hivemq-edge
2. Click "Actions" tab
3. Click "CI Check" workflow on the left
4. Find your branch's run
5. Click on the run to see job details
6. Expand "check-for-changes" to see debug output
7. Check if "check-frontend" appears (not just as a skipped dependency)

## Success Criteria

✅ The fix is successful when:

1. Jobs run based on detected changes (not based on dependency status)
2. Frontend-only changes trigger only check-frontend
3. Backend-only changes trigger only check-backend
4. No changes to reusable workflows were needed
5. The final check job validates correctly

---

**Current Status:** Fix implemented, ready for testing on push
**Next Step:** Push this branch and verify workflow execution
