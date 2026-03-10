# Screenshot Update Process

**Last Updated:** 2026-02-13

---

## Purpose

This document describes how to regenerate and update screenshots **without editing any documentation files**. Once screenshots are integrated into documentation with proper filenames, updating them is a simple copy operation.

---

## Key Principle: Filename-Based References

**Documentation references screenshots by filename:**

```markdown
![Description](../assets/screenshots/datahub/datahub-schema-table-empty-state.png)
```

**To update the screenshot:**

1. Regenerate `datahub-schema-table-empty-state.png` with same name
2. Copy to `docs/assets/screenshots/datahub/`
3. **Done!** - No documentation edits needed

The markdown automatically shows the updated image because the filename hasn't changed.

---

## Screenshot Update Workflow

### Step 1: Run Screenshot Test

```bash
# Run the screenshot test to regenerate images
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"
```

**Output location:**

```
cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/
├── datahub-designer-canvas-empty.png
├── datahub-schema-table-empty-state.png
├── datahub-schema-table-with-data.png
├── datahub-policy-table-empty-state.png
└── datahub-script-table-empty-state.png
```

### Step 2: Copy to Documentation Assets

```bash
# Copy all screenshots to docs (overwrites existing)
cp cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/*.png \
   docs/assets/screenshots/datahub/
```

**Result:**

```
docs/assets/screenshots/datahub/
├── datahub-designer-canvas-empty.png          # Updated ✅
├── datahub-schema-table-empty-state.png       # Updated ✅
├── datahub-schema-table-with-data.png         # Updated ✅
├── datahub-policy-table-empty-state.png       # Updated ✅
└── datahub-script-table-empty-state.png       # Updated ✅
```

### Step 3: Verify in Documentation

**No edits needed!** Just verify the images appear:

```bash
# Open documentation to verify
open docs/architecture/DATAHUB_ARCHITECTURE.md

# Or check in browser if using a markdown preview
```

The documentation automatically shows updated screenshots because:

- Filenames are the same
- Paths haven't changed
- Markdown references are stable

---

## When UI Changes

### Scenario: DataHub UI Updated

**What happens:**

1. Developer updates DataHub UI (new button, layout change, etc.)
2. Current screenshots become outdated

**How to update:**

```bash
# 1. Run test (captures new UI)
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"

# 2. Copy to docs (overwrites old screenshots)
cp cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/*.png \
   docs/assets/screenshots/datahub/

# 3. Done! Documentation now shows updated UI
```

**No markdown edits required** because filenames and paths are unchanged.

---

## When Adding New Screenshots

### If Screenshot Test Adds New Captures

**Example:** Test now captures `datahub-validation-report-success.png`

**Step 1: Run test (generates new file)**

```bash
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"
```

**Step 2: Copy to docs**

```bash
cp cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/datahub-validation-report-success.png \
   docs/assets/screenshots/datahub/
```

**Step 3: Add to documentation** (this step DOES require editing)

```markdown
**Figure 4: Validation Success Report**

![Validation panel showing successful policy check with publish button](../assets/screenshots/datahub/datahub-validation-report-success.png)
```

**Step 4: Update screenshot index**
Edit `docs/assets/screenshots/INDEX.md`:

```markdown
| `datahub-validation-report-success.png` | Validation success panel | **DATAHUB_ARCHITECTURE.md** (Figure 4) | Test source |
```

---

## Automation Scripts (Future)

### Copy Script

**Create:** `tools/copy-screenshots-to-docs.sh`

```bash
#!/usr/bin/env bash
# Copy screenshots from Cypress output to documentation assets

CYPRESS_DIR="cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts"
DOCS_DIR="docs/assets/screenshots/datahub"

if [ ! -d "$CYPRESS_DIR" ]; then
  echo "Error: No screenshots found at $CYPRESS_DIR"
  echo "Run the test first: pnpm cypress:run:e2e --spec \"cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts\""
  exit 1
fi

echo "Copying screenshots to documentation..."
cp "$CYPRESS_DIR"/*.png "$DOCS_DIR/"

echo "✓ Screenshots updated:"
ls -lh "$DOCS_DIR"/*.png

echo ""
echo "Documentation automatically reflects updated screenshots!"
```

**Usage:**

```bash
# Run test, then copy
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"
./tools/copy-screenshots-to-docs.sh
```

### Combined Script

**Add to package.json:**

```json
{
  "scripts": {
    "docs:screenshots:update": "pnpm cypress:run:e2e --spec 'cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts' && ./tools/copy-screenshots-to-docs.sh"
  }
}
```

**Usage:**

```bash
# One command to regenerate and copy
pnpm docs:screenshots:update
```

---

## Quarterly Review Process

### Every Quarter: Verify Screenshots

```bash
# 1. Regenerate all screenshots
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"

# 2. Compare with existing
diff -r cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/ \
        docs/assets/screenshots/datahub/

# 3. If UI changed, copy new screenshots
cp cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/*.png \
   docs/assets/screenshots/datahub/

# 4. Commit if changed
git add docs/assets/screenshots/datahub/
git commit -m "docs: update DataHub screenshots for UI changes"
```

---

## Troubleshooting

### Problem: Screenshots Show Privacy Modal

**Symptom:** All screenshots show "Privacy Settings" cookie consent

**Fix:** Update test to dismiss modal before capturing:

```typescript
beforeEach(() => {
  // ... existing setup ...

  // Dismiss privacy modal
  cy.visit('/')
  cy.get('button:contains("Accept all")').click({ force: true })
})
```

**Check other E2E tests** to see how they handle the privacy modal.

### Problem: Screenshots are Identical

**Symptom:** All PNGs are the same size and show same content

**Cause:** Something is blocking the UI (modal, loading spinner, etc.)

**Fix:** Add proper wait conditions in test:

```typescript
it('should capture schema table', () => {
  cy.visit('/datahub/schemas')

  // Wait for table to render
  cy.get('table[aria-label="List of schemas"]').should('be.visible')
  cy.wait(800) // Allow render to stabilize

  cy.screenshot('datahub-schema-table-empty-state')
})
```

### Problem: Paths Don't Resolve in WebStorm

**Symptom:** "Cannot resolve directory '..'"

**Check:**

1. Filename matches exactly (including `datahub-` prefix)
2. Path is relative from markdown file location
3. File actually exists

**Verify:**

```bash
cd docs/architecture
ls -la ../assets/screenshots/datahub/datahub-schema-table-empty-state.png
```

---

## Benefits of This Approach

### 1. No Documentation Edits for Updates

✅ **Update once (copy files), documentation auto-updates**
❌ No need to edit markdown files
❌ No risk of broken references
❌ No git conflicts in documentation

### 2. Version Control Friendly

- Screenshots are binary files (store in Git LFS if needed)
- Filename-based references are stable
- Easy to see what changed: `git diff docs/assets/screenshots/`

### 3. Consistent Quality

- Same test produces consistent screenshots
- HD viewport (1280x720) enforced
- Same naming convention
- Reproducible

### 4. Fast Updates

```bash
# Entire update process:
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"
cp cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/*.png docs/assets/screenshots/datahub/

# Done! ~30 seconds total
```

---

## Current Issues to Resolve

### ⚠️ Privacy Modal Blocking Screenshots

**Status:** Identified, not yet fixed

**Issue:** Privacy/cookie consent modal appears before content loads

**Next Steps:**

1. Check how other E2E tests dismiss the modal
2. Update `beforeEach()` in screenshot test to dismiss modal
3. Re-run test to capture actual content
4. Copy updated screenshots to docs

**Example from other tests to check:**

```bash
# Search for how other tests handle modals
grep -r "Privacy\|cookie\|consent\|Accept all" cypress/e2e/
```

---

## Summary

**To update screenshots after UI changes:**

```bash
# Step 1: Run test
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"

# Step 2: Copy screenshots
cp cypress/screenshots/datahub-documentation-screenshots.spec.cy.ts/*.png \
   docs/assets/screenshots/datahub/

# Step 3: There is no step 3!
```

**Documentation automatically shows updated screenshots because:**

- ✅ Filenames are stable (same names)
- ✅ Paths are stable (relative paths)
- ✅ Markdown references unchanged
- ✅ No edits required

---

**Last Updated:** 2026-02-13
**Next Action:** Fix privacy modal issue in screenshot test
