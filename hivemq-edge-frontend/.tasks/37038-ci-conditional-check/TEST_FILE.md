# Test File for CI Conditional Check Fix

This file is created to test the conditional workflow execution.

**Purpose:** Trigger frontend changes without triggering backend or OpenAPI changes.

**Expected Behavior:**

- ✅ check-for-changes detects: `frontend-changed: true`
- ⏭️ backend-changed: false
- ⏭️ openapi-changed: false
- ✅ check-frontend should RUN
- ⏭️ check-backend should SKIP
- ⏭️ check-openapi should SKIP

**Test Date:** December 4, 2025

---

## Why This Tests the Fix

The `.github/workflows/check.yml` file modification triggers ALL three filters because each filter includes `.github/**`.

This test file is in `hivemq-edge-frontend/.tasks/` which:

- ✅ Matches `hivemq-edge-frontend/**` (frontend filter)
- ❌ Does NOT match backend paths
- ❌ Does NOT match openapi paths
- ❌ Does NOT match `.github/**`

Therefore, this should trigger ONLY the frontend workflow.

---

## How to Use This Test

1. **First commit:** Include the workflow changes + this file
   - Result: All three run (because of .github/\*\* change)
2. **Second commit:** Modify ONLY this file (add content below)
   - Result: Only frontend should run (proves the fix works!)

---

## Test Content Area

Add lines below this section to create frontend-only changes:

<!-- Test iterations -->

- Test iteration 1: Initial file creation
- Test iteration 2: Verifying frontend-only detection - 2025-12-04 14:11:31
