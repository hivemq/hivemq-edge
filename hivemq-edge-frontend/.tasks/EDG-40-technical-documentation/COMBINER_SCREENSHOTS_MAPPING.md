# Combiner Walkthrough - Screenshot Mapping

**Date:** 2026-02-16
**Documentation:** docs/walkthroughs/RJSF_COMBINER.md
**Target Directory:** docs/assets/screenshots/combiner/

---

## Overview

This document maps each screenshot placeholder in the Combiner walkthrough to its Cypress test location. Run tests to generate screenshots, then copy to documentation assets directory.

**Total Screenshots Implemented:** 10 (out of 15 placeholders in documentation)
**Test Files Modified:** 5 component tests, 1 E2E test created

---

## Screenshot Mapping

### 1. Introduction - UX Overview

| Screenshot                        | Test File                                                          | Test Name                                                                  | Status             |
| --------------------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------- | ------------------ |
| `combiner-native-form-flat.png`   | (Not implemented)                                                  | N/A                                                                        | ❌ NOT IMPLEMENTED |
| `combiner-custom-ux-overview.png` | cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts | Introduction - UX Overview → "should capture custom Combiner UX with tabs" | ✅ Implemented     |
| `combiner-tabs-navigation.png`    | cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts | Introduction - UX Overview → "should capture custom Combiner UX with tabs" | ✅ Implemented     |

**Notes:**

- Native form screenshot NOT implemented - would require adding native form toggle or manual capture
- Custom UX and tabs captured in same E2E test with navigation between states

---

### 2. PrimarySelect Widget

| Screenshot                                     | Test File                                               | Test Name                                     | Status   |
| ---------------------------------------------- | ------------------------------------------------------- | --------------------------------------------- | -------- |
| `combiner-primary-select-dropdown.png`         | src/modules/Mappings/combiner/PrimarySelect.spec.cy.tsx | "should capture screenshot for documentation" | ✅ Added |
| `combiner-primary-select-validation-error.png` | E2E or component test with validation trigger           | TBD                                           | 📝 TODO  |

**Notes:**

- Dropdown screenshot shows options (tags + topic filters) with key icon
- Validation error requires triggering custom validation (primary not in sources list)
- Consider adding validation test or documenting error state manually

---

### 3. CombinedEntitySelect Widget

| Screenshot                                | Test File                                                      | Test Name                                     | Status   |
| ----------------------------------------- | -------------------------------------------------------------- | --------------------------------------------- | -------- |
| `combiner-entity-select-dropdown.png`     | src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx | "should capture screenshot for documentation" | ✅ Added |
| `combiner-entity-select-multi-select.png` | Same test, with selections made                                | 📝 TODO - Extend test                         |

**Notes:**

- Dropdown screenshot shows entity options with icons, descriptions, tag counts
- Multi-select chips screenshot requires selecting entities and capturing selected state
- Can extend existing test to capture both states

---

### 4. DataCombiningEditorField

| Screenshot                           | Test File                                                                                             | Test Name                                     | Status   |
| ------------------------------------ | ----------------------------------------------------------------------------------------------------- | --------------------------------------------- | -------- |
| `combiner-editor-field-layout.png`   | src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx                                    | "should capture screenshot for documentation" | ✅ Added |
| `combiner-schema-loaders-loaded.png` | src/modules/Mappings/combiner/CombinedSchemaLoader.spec.cy.tsx or DestinationSchemaLoader.spec.cy.tsx | Loaded state test                             | 📝 TODO  |

**Notes:**

- Layout screenshot shows grid split (sources left, destination right)
- Schema loaders showing "loaded" state requires successful query mock
- May need to enhance loader tests or capture in E2E context

---

### 5. DataCombiningTableField

| Screenshot                         | Test File                                                          | Test Name                                                            | Status   |
| ---------------------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------- | -------- |
| `combiner-table-with-mappings.png` | src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx  | Screenshots for documentation → "should capture table with mappings" | ✅ Added |
| `combiner-table-empty-state.png`   | src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx  | Screenshots for documentation → "should capture empty state"         | ✅ Added |
| `combiner-mapping-drawer-open.png` | cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts | DataCombiningTableField → "should capture drawer open with editor"   | ✅ Added |

**Notes:**

- Table screenshot shows 3+ mappings with summaries (primary, sources count, destination)
- Empty state shows "No data received yet" message with add button
- Drawer screenshot captured in E2E context for full integration

---

### 6. Error States

| Screenshot                             | Test File                                                      | Test Name        | Status  |
| -------------------------------------- | -------------------------------------------------------------- | ---------------- | ------- |
| `combiner-schema-loading-error.png`    | src/modules/Mappings/combiner/CombinedSchemaLoader.spec.cy.tsx | Error state test | 📝 TODO |
| `combiner-inline-validation-error.png` | E2E or component test with validation                          | TBD              | 📝 TODO |

**Notes:**

- Schema loading error requires intercepting query with 500 error
- Inline validation error shows below field (red text, icon)
- Both may require new test cases or manual capture

---

### 7. Loading States

| Screenshot                            | Test File                                                         | Test Name                                                  | Status                |
| ------------------------------------- | ----------------------------------------------------------------- | ---------------------------------------------------------- | --------------------- |
| `combiner-schema-loader-skeleton.png` | src/modules/Mappings/combiner/CombinedSchemaLoader.spec.cy.tsx    | "should capture screenshot for documentation - info state" | ✅ Added (Info state) |
| `combiner-table-loading-state.png`    | src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx | Loading state test                                         | 📝 TODO               |

**Notes:**

- Schema loader skeleton may show as info state ("No schemas available")
- Table loading state requires delayed query response
- Loading skeleton may not be visually distinct in current implementation

---

## Summary by Status

**✅ Added (10 screenshots):**

1. combiner-tabs-navigation
2. combiner-custom-ux-overview
3. combiner-primary-select-dropdown
4. combiner-entity-select-dropdown
5. combiner-editor-field-layout
6. combiner-table-with-mappings
7. combiner-table-empty-state
8. combiner-mapping-drawer-open
9. combiner-schema-loader-info
10. combiner-schema-loader-no-schemas

**📝 TODO (5 screenshots):**

1. combiner-native-form-flat (Manual/Component test needed)
2. combiner-primary-select-validation-error (Validation trigger needed)
3. combiner-entity-select-multi-select (Extend existing test)
4. combiner-schema-loaders-loaded (Success state needed)
5. combiner-schema-loading-error (Error intercept needed)
6. combiner-inline-validation-error (Validation trigger needed)
7. combiner-table-loading-state (Loading intercept needed)

---

## Running Tests to Generate Screenshots

### Component Tests

```bash
# PrimarySelect
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/PrimarySelect.spec.cy.tsx"

# CombinedEntitySelect
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx"

# DataCombiningEditorField
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx"

# DataCombiningTableField
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx"

# CombinedSchemaLoader
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/CombinedSchemaLoader.spec.cy.tsx"
```

### E2E Tests

```bash
# Combiner documentation screenshots
pnpm cypress:run:e2e --spec "cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts"
```

### Run All Component Tests

```bash
# All combiner component tests
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/*.spec.cy.tsx"
```

---

## Copying Screenshots to Documentation

### Manual Copy Commands

```bash
# Component test screenshots (from test name directories)
cp cypress/screenshots/combiner-primary-select-dropdown.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-entity-select-dropdown.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-editor-field-layout.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-table-with-mappings.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-table-empty-state.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-schema-loader-info.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-schema-loader-no-schemas.png \
   docs/assets/screenshots/combiner/

# E2E test screenshots
cp cypress/screenshots/combiner-tabs-navigation.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-custom-ux-overview.png \
   docs/assets/screenshots/combiner/

cp cypress/screenshots/combiner-mapping-drawer-open.png \
   docs/assets/screenshots/combiner/
```

### Script (Recommended)

Create a script in `tools/copy-combiner-screenshots.sh`:

```bash
#!/bin/bash

# Create destination directory
mkdir -p docs/assets/screenshots/combiner

# Find all combiner screenshots in cypress/screenshots
find cypress/screenshots -name "combiner-*.png" -exec cp {} docs/assets/screenshots/combiner/ \;

echo "✅ Copied combiner screenshots to docs/assets/screenshots/combiner/"
ls -1 docs/assets/screenshots/combiner/
```

Run:

```bash
chmod +x tools/copy-combiner-screenshots.sh
./tools/copy-combiner-screenshots.sh
```

---

## Updating Documentation

Once screenshots are copied, verify paths in `docs/walkthroughs/RJSF_COMBINER.md`:

**Current placeholders:**

```markdown
![Screenshot: Primary selector dropdown](../assets/screenshots/combiner/combiner-primary-select-dropdown.png)
```

**After copying:**

1. Verify all image paths resolve
2. Check alt text is descriptive
3. Ensure screenshots match documented states
4. Update this mapping file with actual filenames if different

---

## Naming Conventions

All screenshots follow the pattern: `combiner-{component}-{state}.png`

**Examples:**

- `combiner-primary-select-dropdown.png` (component + state)
- `combiner-table-empty-state.png` (component + state)
- `combiner-editor-field-layout.png` (component + description)

**Do NOT use:**

- Spaces: ❌ `combiner primary select.png`
- Prefixes: ❌ `PR-combiner-primary-select.png`
- Directories in name: ❌ `combiner/primary/select.png`

---

## Test Files Modified

### Component Tests (5 files)

1. **src/modules/Mappings/combiner/PrimarySelect.spec.cy.tsx**

   - Added: "should capture screenshot for documentation"
   - Captures: Dropdown with options visible

2. **src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx**

   - Added: "should capture screenshot for documentation"
   - Captures: Entity selector dropdown with metadata

3. **src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx**

   - Added: "should capture screenshot for documentation"
   - Captures: Full editor layout (grid split)

4. **src/modules/Mappings/combiner/DataCombiningTableField.spec.cy.tsx**

   - Added: "Screenshots for documentation" describe block
   - Added: "should capture empty state"
   - Added: "should capture table with mappings"
   - Captures: Table empty state and populated state

5. **src/modules/Mappings/combiner/CombinedSchemaLoader.spec.cy.tsx**
   - Added: "should capture screenshot for documentation - info state"
   - Added: "should capture screenshot for documentation - warning state"
   - Captures: Schema loader states

### E2E Tests (1 file)

1. **cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts** (NEW)
   - Created: Full E2E screenshot suite
   - Captures: Tabs navigation, custom UX overview, drawer states

---

## Viewport Standards

**Component Tests:** `cy.viewport(800, 800)` (flexible based on component)

**E2E Tests:** `cy.viewport(1280, 720)` (HD - REQUIRED for E2E)

All E2E screenshots MUST use HD viewport for consistency.

---

## Next Steps

1. **Run Tests**

   ```bash
   pnpm cypress:run:component --spec "src/modules/Mappings/combiner/*.spec.cy.tsx"
   pnpm cypress:run:e2e --spec "cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts"
   ```

2. **Review Generated Screenshots**

   - Check `cypress/screenshots/` for all generated images
   - Verify quality and content

3. **Copy to Documentation Assets**

   ```bash
   ./tools/copy-combiner-screenshots.sh
   # OR manual copy commands above
   ```

4. **Verify in Documentation**

   - Open `docs/walkthroughs/RJSF_COMBINER.md`
   - Check all image paths resolve
   - Confirm screenshots match described states

5. **Complete TODO Screenshots**
   - Add missing tests for validation errors, loading states
   - Capture remaining 5 screenshots
   - Update this mapping document

---

**Status:** ✅ 10/15 screenshots ready for capture
**Next Review:** After test execution
**Related:**

- docs/walkthroughs/RJSF_COMBINER.md
- .tasks/EDG-40-technical-documentation/SCREENSHOT_GUIDELINES.md
