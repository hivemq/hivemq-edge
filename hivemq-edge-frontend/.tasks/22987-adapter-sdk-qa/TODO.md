# TODO - Adapter SDK QA Task

## Completed

- [x] JSON Schema Configuration Guide
- [x] UI Schema Configuration Guide
- [x] Adapter QA Checklist
- [x] Visual Testing Suite (Java server + React frontend)
- [x] Cypress automated tests (38 tests across 3 files)

## Completed

- [x] CI Pipeline for Automated QA Reports

### CI Pipeline Usage

```bash
cd hivemq-edge-adapter-sdk/testing/ui/frontend

# Run full QA check (tests + report)
npm run qa:check

# Or individual steps:
npm run cypress:ci    # Run tests with JSON output
npm run qa:merge      # Merge test results
npm run qa:report     # Generate developer-friendly report
```

**Report includes:**

- Pass/fail status per checklist item
- Rationale explaining why each check matters
- Suggested fixes for failures
- Severity classification (critical/high/medium/low)

**Output:**

- Console: Formatted report with severity icons
- File: `qa-report.json` for programmatic use

## Pending

### 2. Frontend Fix: Duplicate Field Labels

**Issue:** Field labels (titles) are rendered twice in adapter configuration forms.

**Root Cause:** The custom `FieldTemplate.tsx` renders the label via `FormLabel`, but `@rjsf/chakra-ui` already renders labels in its built-in templates. This causes duplication.

**Location:** `src/components/rjsf/Form/FieldTemplate.tsx`

**Fix Applied in Testing UI:**
Removed the custom `FieldTemplate` from the RJSF templates configuration. The `@rjsf/chakra-ui` package provides adequate field templates with proper label handling.

```tsx
// Before (duplicate labels)
<Form
  templates={{
    ObjectFieldTemplate,
    FieldTemplate,  // <-- causes duplicate labels
  }}
/>

// After (fixed)
<Form
  templates={{
    ObjectFieldTemplate,
    // Let @rjsf/chakra-ui handle field templates
  }}
/>
```

**Action Required:**
Review the Edge frontend's `FieldTemplate.tsx` to determine if:

1. It can be removed entirely (preferred if no custom behavior is needed)
2. Or modified to not render labels when `@rjsf/chakra-ui` already handles them

**Files to Review:**

- `/hivemq-edge-frontend/src/components/rjsf/Form/FieldTemplate.tsx`
- `/hivemq-edge-frontend/src/components/rjsf/Form/ChakraRJSForm.tsx`

## Test Coverage Summary

| Test File                | Count  | Coverage                                              |
| ------------------------ | ------ | ----------------------------------------------------- |
| `1-json-schema.cy.ts`    | 13     | Field metadata, constraints, required, enums, formats |
| `2-ui-schema.cy.ts`      | 12     | Tabs, order, widgets, field behavior, arrays          |
| `6-visual-testing.cy.ts` | 13     | Rendering, validation, accessibility                  |
| **Total**                | **38** |                                                       |

Tests map to QA checklist items (e.g., `[1.1.1]` = checklist item 1.1.1)
