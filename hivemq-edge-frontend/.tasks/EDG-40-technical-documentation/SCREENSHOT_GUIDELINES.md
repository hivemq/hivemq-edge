# Screenshot Guidelines for Documentation

**Task:** EDG-40-technical-documentation
**Last Updated:** 2026-02-13

---

## Purpose

This document defines standards for capturing, naming, storing, and using screenshots in documentation. Screenshots complement Mermaid diagrams by showing actual UI implementation and visual states.

---

## When to Use Screenshots vs Diagrams

### ✅ Use Screenshots When:

1. **Showing Actual UI Implementation**

   - Visual representation of components, layouts, states
   - Proof that features are implemented as designed
   - Complex UI interactions that are hard to describe

2. **Demonstrating Visual States**

   - Error states, loading states, success states
   - Before/after comparisons
   - Different data scenarios (empty state, populated, edge cases)

3. **Workflow Documentation**

   - Step-by-step user flows
   - Multi-step wizards
   - Configuration panels with actual data

4. **Visual Regression Evidence**
   - UI changes for PR documentation
   - Accessibility improvements (color contrast, focus indicators)
   - Layout fixes

### ❌ Use Mermaid Diagrams Instead When:

1. **Showing Architecture/Data Flow**

   - Component relationships
   - State management flow
   - API request/response patterns
   - Decision trees

2. **Abstract Concepts**

   - Process workflows (not UI workflows)
   - System architecture
   - Database schemas

3. **Information That Changes Frequently**
   - Screenshots become outdated quickly
   - Diagrams are easier to update

### 📊 Limit Screenshot Usage

**Like diagrams, avoid overusing screenshots:**

- Maximum 1-2 screenshots per major section
- Only when they add significant value
- Prefer diagrams for architecture, screenshots for UI

---

## Screenshot Quality Requirements

### ⚠️ CRITICAL: Viewport Standard

**ALL E2E TESTS AND DOCUMENTATION SCREENSHOTS MUST USE HD (1280x720)**

This is a **mandatory requirement** for consistency and responsive testing.

### Viewport & Resolution

**STANDARD: HD (1280x720) - REQUIRED FOR E2E**

All E2E tests and documentation screenshots MUST use HD resolution:

```typescript
cy.viewport(1280, 720) // Standard HD viewport - REQUIRED for E2E
```

**Rationale:**

- Consistent screenshot sizing across all documentation
- Tests responsive behavior at common desktop size
- Screenshots fit well in documentation without scrolling
- Aligns with responsive testing strategy

**Component Tests (Flexible):**

```typescript
cy.viewport(800, 800) // Common for component tests (can vary based on component)
cy.viewport(600, 400) // Smaller components
cy.viewport(1200, 800) // Larger components
```

Component tests can use any appropriate viewport size based on the component being tested.

**Full HD (1920x1080) - EXCEPTIONAL CASES ONLY:**

```typescript
cy.viewport(1920, 1080) // Use ONLY when HD doesn't show necessary detail
```

**When to use Full HD:**

- Complex multi-panel layouts that require more space
- Demonstrating wide-screen specific features
- **Must be justified** - default to HD

**Mobile/Responsive Testing (Not for standard documentation):**

```typescript
cy.viewport(375, 667) // iPhone SE
cy.viewport(768, 1024) // iPad
```

Use mobile viewports for responsive testing, but not for standard documentation screenshots unless specifically documenting mobile behavior.

### Capture Options

**Standard E2E Screenshot (REQUIRED pattern):**

```typescript
// Set HD viewport first
cy.viewport(1280, 720)

// Capture full viewport
cy.screenshot('screenshot-name', {
  overwrite: true,
  capture: 'viewport',
})
```

**Component Screenshot (Flexible viewport):**

```typescript
// Set appropriate viewport for component size
cy.viewport(800, 800)

// Capture full viewport
cy.screenshot('component-name-state', {
  overwrite: true,
  capture: 'viewport',
})
```

**Specific Element (When Needed):**

```typescript
cy.get('[data-testid="policy-designer"]').screenshot('designer-canvas', {
  overwrite: true,
  capture: 'runner',
})
```

**⚠️ Full HD Exception (Rare):**

```typescript
// ONLY when HD (1280x720) doesn't show necessary detail
// Must document why Full HD is needed
cy.viewport(1920, 1080)
cy.screenshot('complex-multi-panel-layout', {
  overwrite: true,
  capture: 'viewport',
})
```

### Visual Clarity

- **Clean State:** Remove development tools, browser chrome
- **Realistic Data:** Use meaningful test data, not "foo", "bar"
- **Focus:** Screenshot should show what you're documenting, not extra UI
- **Annotations:** Consider adding arrows/highlights in documentation (not in screenshot itself)

---

## Naming Convention

### Pattern: `{feature}-{state}-{description}`

**Rules:**

- All lowercase
- Use hyphens (not spaces, slashes, or underscores)
- Descriptive but concise (max 50 chars)
- Include state if showing specific UI state

### Examples

**Component Screenshots:**

```typescript
// ✅ Good
cy.screenshot('schema-table-empty-state')
cy.screenshot('schema-table-with-data')
cy.screenshot('schema-editor-validation-error')
cy.screenshot('policy-designer-canvas-clean')
cy.screenshot('policy-designer-with-nodes')

// ❌ Bad
cy.screenshot('Screenshot 1') // Not descriptive
cy.screenshot('Workspace Wizard / Bridge wizard') // Uses slashes
cy.screenshot('PR-Screenshot-1-Healthy-Workspace') // Prefix not needed
cy.screenshot('after/after-modal-empty-state') // Directory in name
```

**E2E Workflow Screenshots:**

```typescript
// ✅ Good - Numbered workflow steps
cy.screenshot('adapter-wizard-01-menu')
cy.screenshot('adapter-wizard-02-type-selection')
cy.screenshot('adapter-wizard-03-configuration')
cy.screenshot('adapter-wizard-04-success')

// ✅ Good - State-based
cy.screenshot('workspace-layout-before-radial')
cy.screenshot('workspace-layout-after-radial')
cy.screenshot('duplicate-combiner-modal-empty')
cy.screenshot('duplicate-combiner-modal-populated')
```

---

## Directory Structure

### Test Screenshot Location

**During Test Execution:**
Screenshots are automatically saved to:

```
cypress/screenshots/{test-path}/{test-name}/{screenshot-name}.png
```

### Documentation Screenshot Location

**For Documentation Usage:**
Copy screenshots to:

```
docs/assets/screenshots/{feature-domain}/{screenshot-name}.png
```

**Structure - Organized by Feature/Domain:**

```
docs/assets/screenshots/
├── datahub/                # DataHub extension screenshots
│   ├── designer-canvas-empty.png
│   ├── designer-canvas-with-nodes.png
│   ├── schema-table-empty-state.png
│   ├── schema-table-with-data.png
│   ├── schema-editor-validation-error.png
│   └── policy-validation-success.png
├── workspace/              # Workspace/topology screenshots
│   ├── workspace-healthy-all-operational.png
│   ├── workspace-layout-before-radial.png
│   ├── workspace-layout-after-radial.png
│   ├── wizard-01-menu.png
│   ├── wizard-02-adapter-selection.png
│   └── duplicate-combiner-modal-empty.png
├── adapters/               # Protocol adapter screenshots
│   ├── adapter-configuration-panel.png
│   └── adapter-list-table.png
├── bridges/                # Bridge configuration screenshots
│   └── bridge-wizard-configuration.png
├── ui-components/          # Shared UI component screenshots
│   ├── button-variants.png
│   ├── form-states-example.png
│   └── table-pagination-example.png
├── development/            # Development tools/processes
│   ├── dev-server-running.png
│   ├── cypress-test-runner.png
│   └── ci-pipeline-success.png
└── common/                 # Common UI elements/patterns
    └── empty-state-example.png
```

**Rationale:**

- **Reusability:** Screenshots organized by what they show, not where they're used
- **Discoverability:** Feature-based organization matches codebase structure
- **Flexibility:** Same screenshot can be referenced in multiple documents
- **Scalability:** Easy to add new features without reorganizing

**Examples:**

```markdown
<!-- In docs/architecture/DATAHUB_ARCHITECTURE.md -->

![Policy designer canvas](../assets/screenshots/datahub/designer-canvas-empty.png)

<!-- In docs/guides/DATAHUB_GUIDE.md -->

![Policy designer canvas](../assets/screenshots/datahub/designer-canvas-empty.png)

<!-- Both reference the same file! -->
```

### Screenshot Index (Recommended)

**Maintain a screenshot index for easy reference:**

**File:** `docs/assets/screenshots/INDEX.md`

```markdown
# Screenshot Index

## DataHub

- `datahub/designer-canvas-empty.png` - Empty policy designer canvas
- `datahub/schema-table-empty-state.png` - Schema table with no data

## Workspace

- `workspace/workspace-healthy-all-operational.png` - Healthy workspace view
- `workspace/wizard-01-menu.png` - Workspace wizard menu

## Usage

Each screenshot can be referenced from multiple documents using relative paths:
`![Description](../assets/screenshots/{domain}/{filename}.png)`
```

### Copy Script (Add to package.json)

```json
{
  "scripts": {
    "docs:screenshots:copy": "node tools/copy-screenshots-to-docs.js",
    "docs:screenshots:index": "node tools/generate-screenshot-index.js"
  }
}
```

---

## Adding Screenshots to Tests

### Component Test Example

**File:** `src/extensions/datahub/components/pages/SchemaTable.spec.cy.tsx`

```typescript
describe('SchemaTable', () => {
  it('should capture screenshot for documentation', () => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/data-hub/schemas', {
      items: [mockSchemaTempHumidity]
    })

    cy.mountWithProviders(<SchemaTable />)
    cy.wait(500) // Allow render to stabilize

    // Capture screenshot for docs
    cy.screenshot('schema-table-with-data', {
      overwrite: true,
      capture: 'viewport'
    })
  })

  it('should capture empty state for documentation', () => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/schemas', { items: [] })

    cy.mountWithProviders(<SchemaTable />)
    cy.wait(500)

    cy.screenshot('schema-table-empty-state', {
      overwrite: true,
      capture: 'viewport'
    })
  })
})
```

### E2E Test Example

**File:** `cypress/e2e/datahub/policy-designer-workflow.spec.cy.ts`

```typescript
describe('Policy Designer Workflow', () => {
  it('should document complete policy creation workflow', () => {
    // REQUIRED: HD viewport for all E2E screenshots
    cy.viewport(1280, 720)

    // Step 1: Empty canvas
    cy.visit('/datahub/data-policies/new')
    cy.wait('@getPolicySchema')
    cy.screenshot('policy-designer-01-empty-canvas', {
      overwrite: true,
      capture: 'viewport',
    })

    // Step 2: Add nodes
    cy.getByTestId('toolbox-validator').drag('[data-testid="canvas"]')
    cy.wait(300)
    cy.screenshot('policy-designer-02-with-validator', {
      overwrite: true,
      capture: 'viewport',
    })

    // Step 3: Validation result
    cy.getByTestId('toolbar-validate').click()
    cy.wait('@validatePolicy')
    cy.screenshot('policy-designer-03-validation-result', {
      overwrite: true,
      capture: 'viewport',
    })
  })
})
```

### Dedicated Screenshot Test

**File:** `cypress/e2e/{feature}/{feature}-documentation-screenshots.spec.cy.ts`

```typescript
/**
 * This test suite captures screenshots specifically for documentation.
 * Screenshots are used in docs/architecture/DATAHUB_ARCHITECTURE.md
 *
 * IMPORTANT: All E2E screenshots MUST use HD viewport (1280x720)
 */
describe('DataHub - Documentation Screenshots', () => {
  beforeEach(() => {
    // REQUIRED: HD viewport for consistent documentation screenshots
    cy.viewport(1280, 720)
    cy_interceptCoreE2E()
  })

  it('should capture policy designer canvas states', () => {
    // Clean canvas
    cy.visit('/datahub/data-policies/new')
    cy.screenshot('datahub-designer-canvas-empty', {
      overwrite: true,
      capture: 'viewport',
    })

    // With nodes
    // ... add nodes via toolbox
    cy.screenshot('datahub-designer-canvas-populated', {
      overwrite: true,
      capture: 'viewport',
    })
  })

  it('should capture schema table states', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchema] })
    cy.visit('/datahub/schemas')

    cy.screenshot('datahub-schema-table-with-data', {
      overwrite: true,
      capture: 'viewport',
    })
  })
})
```

---

## Using Screenshots in Documentation

### Markdown Syntax

**Basic Image:**

```markdown
![Policy Designer Canvas](../assets/screenshots/datahub/designer-canvas-empty.png)
```

**With Caption:**

```markdown
**Figure 1: Policy Designer Canvas - Empty State**

![Empty policy designer canvas showing toolbox and clean React Flow workspace](../assets/screenshots/datahub/designer-canvas-empty.png)

The policy designer starts with a clean canvas. Users drag nodes from the toolbox (left) onto the canvas to build policies.
```

**Side-by-Side Comparison:**

```markdown
| Before                                                                                      | After                                                                                     |
| ------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| ![Before radial layout](../assets/screenshots/workspace/workspace-layout-before-radial.png) | ![After radial layout](../assets/screenshots/workspace/workspace-layout-after-radial.png) |
| Workspace with default hierarchical layout                                                  | Workspace after applying radial layout algorithm                                          |
```

**Reusing in Multiple Documents:**

```markdown
<!-- In docs/architecture/DATAHUB_ARCHITECTURE.md -->

![DataHub policy designer](../assets/screenshots/datahub/designer-canvas-empty.png)

<!-- In docs/guides/DATAHUB_QUICK_START.md -->

![Start with an empty canvas](../assets/screenshots/datahub/designer-canvas-empty.png)

<!-- Same screenshot, different context! -->
```

### Documentation Examples

#### Example 1: Architecture Document

**File:** `docs/architecture/DATAHUB_ARCHITECTURE.md`

```markdown
## Component Architecture

### Policy Designer Canvas

The policy designer uses React Flow to provide a visual node-based editor.

**Figure: Policy Designer Interface**

![Policy designer showing validator and schema nodes connected on canvas](../assets/screenshots/datahub/designer-canvas-with-nodes.png)

**Key Components:**

1. **Toolbox** (left): Drag-and-drop node library
2. **Canvas** (center): React Flow workspace
3. **Toolbar** (top): Validation and publishing controls
4. **Panel** (right): Node configuration drawer

The screenshot above shows a data policy with:

- One validator node (validates incoming MQTT messages)
- One schema node (defines JSON Schema)
- Connection between nodes (data flow)

See [State Management](#state-management) for how canvas state is managed.
```

#### Example 2: Guide Document

**File:** `docs/guides/TESTING_GUIDE.md`

````markdown
## Component Testing with Cypress

### Screenshot Example

Component tests can verify visual states by capturing screenshots:

![Schema table component showing empty state with "No schemas found" message](../assets/screenshots/datahub/schema-table-empty-state.png)

**Code:**

```typescript
it('should display empty state', () => {
  cy.intercept('/api/v1/data-hub/schemas', { items: [] })
  cy.mountWithProviders(<SchemaTable />)

  cy.contains('No schemas found').should('be.visible')
  cy.screenshot('datahub-schema-table-empty-state')
})
```
````

````

#### Example 3: Reusing Across Documents

**Same screenshot in multiple contexts:**

**File:** `docs/architecture/WORKSPACE_ARCHITECTURE.md`
```markdown
## Workspace Layout Algorithms

The workspace supports multiple layout algorithms. The radial layout arranges nodes in a circular pattern around a central hub:

![Workspace with radial layout showing central edge node](../assets/screenshots/workspace/workspace-layout-after-radial.png)
````

**File:** `docs/guides/WORKSPACE_GUIDE.md`

```markdown
## Changing Workspace Layout

To apply the radial layout:

1. Open the workspace
2. Click the layout dropdown
3. Select "Radial"

The result will look like this:

![Radial layout applied to workspace](../assets/screenshots/workspace/workspace-layout-after-radial.png)
```

**Same file (`workspace/workspace-layout-after-radial.png`), referenced in both architecture and guide documents!**

---

## Integration with Documentation Acceptance Criteria

Screenshots should be added as **Criterion 6** in `DOCUMENTATION_ACCEPTANCE_CRITERIA.md`:

### Criterion 6: Screenshots (When Beneficial) ✅

**Screenshots enhance documentation by showing actual UI implementation and visual states.**

**When to Include:**

- [ ] **UI Components** - Show actual implementation
- [ ] **Visual States** - Error, loading, success, empty states
- [ ] **Workflows** - Step-by-step user journeys
- [ ] **Before/After** - Visual changes, improvements

**Quality Requirements:**

- [ ] **Appropriate Viewport** - 800x800 (component), 1280x720 (E2E)
- [ ] **Clean State** - No dev tools, realistic data
- [ ] **Correct Format** - PNG, stored in `docs/assets/screenshots/{feature-domain}/`
- [ ] **Named Correctly** - `{feature}-{state}-{description}.png`
- [ ] **Referenced Properly** - Alt text describes image content
- [ ] **Organized by Feature** - Grouped by what they show, not where used

**Example:**

```markdown
**Figure: Schema Table - Empty State**

![Schema table showing "No schemas found" message with create button](../assets/screenshots/datahub/schema-table-empty-state.png)
```

**Verification:**

1. Screenshot file exists in correct location
2. Alt text is descriptive
3. Caption explains what screenshot shows
4. Screenshot is referenced in text, not standalone

**Rejection Criteria:**

- ❌ Poor quality (low resolution, cropped incorrectly)
- ❌ Contains sensitive data or internal URLs
- ❌ Missing alt text
- ❌ Stored in test directory (must be in docs/assets/)

---

## Screenshot Workflow

### Step 1: Identify Need

During documentation review, identify sections where screenshots would add value:

- UI components that are hard to describe
- Visual states (errors, empty states)
- Complex workflows

### Step 2: Create or Update Test

**Option A: Add to Existing Test**

```typescript
it('should render the component', () => {
  cy.mountWithProviders(<Component />)
  // existing assertions

  // Add screenshot for docs
  cy.screenshot('component-name-state', { overwrite: true, capture: 'viewport' })
})
```

**Option B: Create Dedicated Screenshot Test**
Create `{feature}-documentation-screenshots.spec.cy.ts` with screenshots only.

### Step 3: Run Test to Generate Screenshot

```bash
# Component test
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"

# E2E test
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"
```

### Step 4: Copy to Documentation Assets

```bash
# Manual copy - organize by feature/domain
cp cypress/screenshots/path/to/datahub-designer-canvas-empty.png \
   docs/assets/screenshots/datahub/designer-canvas-empty.png

cp cypress/screenshots/path/to/workspace-wizard-01-menu.png \
   docs/assets/screenshots/workspace/wizard-01-menu.png

# OR use script (TODO: create this)
pnpm docs:screenshots:copy
```

### Step 5: Add to Documentation

```markdown
**Figure: Description**

![Alt text](../assets/screenshots/{feature-domain}/screenshot-name.png)

Explanation of what screenshot shows and why it's relevant.
```

### Step 6: Update Screenshot Index (Optional)

Add entry to `docs/assets/screenshots/INDEX.md`:

```markdown
## DataHub

- `datahub/designer-canvas-empty.png` - Empty policy designer canvas
  - Used in: DATAHUB_ARCHITECTURE.md, DATAHUB_GUIDE.md
```

### Step 6: Update Tracking

Add screenshot to documentation:

- Note in document's "Last Updated" section
- Track in `MISSING_DOCS_TRACKER.md` if placeholder

---

## Common Patterns

### Empty State

**Test:**

```typescript
it('should show empty state', () => {
  cy.intercept('/api/v1/resource', { items: [] })
  cy.mountWithProviders(<Component />)
  cy.screenshot('component-empty-state')
})
```

**Documentation:**

```markdown
When no data exists, the component displays a helpful empty state:

![Empty state showing "No items found" with create button](../assets/screenshots/guides/component-empty-state.png)
```

### Error State

**Test:**

```typescript
it('should show error state', () => {
  cy.intercept('/api/v1/resource', { statusCode: 500 })
  cy.mountWithProviders(<Component />)
  cy.wait(500) // Allow error to render
  cy.screenshot('component-error-state')
})
```

### Workflow Steps

**Test:**

```typescript
it('should document wizard workflow', () => {
  // Step 1
  cy.visit('/wizard/new')
  cy.screenshot('wizard-01-start')

  // Step 2
  cy.getByTestId('next-button').click()
  cy.screenshot('wizard-02-configuration')

  // Step 3
  cy.getByTestId('submit-button').click()
  cy.screenshot('wizard-03-success')
})
```

---

## Tools & Scripts

### Screenshot Copy Script (TODO)

**File:** `tools/copy-screenshots-to-docs.js`

```javascript
// TODO: Create script to copy screenshots from cypress/screenshots
// to docs/assets/screenshots/ based on naming convention
```

### Screenshot Audit Command

```bash
# Find all screenshots in tests
grep -r "cy.screenshot" src/ cypress/

# List screenshots in docs
find docs/assets/screenshots -type f -name "*.png"

# Find broken image links in docs
grep -r "!\[.*\]" docs/ | grep "screenshots"
```

---

## Migration: Existing Screenshots

### Current Screenshot Inventory

**Found:** 20 screenshots in `cypress/screenshots/`

**Issues:**

- Mixed naming conventions (spaces, slashes, prefixes)
- Many are from failed tests (not intentional documentation)
- No clear documentation purpose

### Migration Plan

1. **Review Existing Screenshots**

   - Identify intentional documentation screenshots
   - Delete failure screenshots (CI artifacts)

2. **Rename for Consistency**

   - Apply new naming convention
   - Remove "PR-" prefixes
   - Replace spaces/slashes with hyphens

3. **Copy to Docs Assets**

   - Organize by section (architecture, guides, etc.)
   - Add to relevant documentation

4. **Update Tests**
   - Ensure tests use consistent naming
   - Add comments explaining screenshot purpose

---

## Best Practices

### DO ✅

1. **Use HD Viewport for E2E (MANDATORY)**

   - Always `cy.viewport(1280, 720)` for E2E tests
   - Ensures consistency across all documentation
   - Tests responsive behavior at standard desktop size
   - Component tests can use flexible viewports

2. **Use Realistic Data**

   - Names: "Temperature Sensor", not "Adapter 1"
   - Values: "23.5°C", not "foo"

3. **Stabilize Before Screenshot**

   - Add `cy.wait(300)` if animations present
   - Wait for network requests to complete

4. **Document Screenshot Purpose**

   - Add comments in tests explaining what screenshot shows
   - Reference in documentation

5. **Keep Screenshots Updated**
   - Re-run tests after UI changes
   - Update docs/assets/ copies

### DON'T ❌

1. **Don't Use Non-Standard Viewports for E2E**

   - ❌ `cy.viewport(1920, 1080)` without justification
   - ❌ `cy.viewport(1366, 768)` or other random sizes
   - ✅ Always use `cy.viewport(1280, 720)` for E2E
   - Exception: Full HD only when HD doesn't show necessary detail

2. **Don't Include Sensitive Data**

   - No real API keys, passwords, URLs
   - Use mocks with safe data

3. **Don't Screenshot Development Tools**

   - No browser devtools
   - No Cypress runner UI (unless documenting Cypress itself)

4. **Don't Over-Screenshot**

   - Maximum 1-2 per major section
   - Prefer diagrams for architecture

5. **Don't Use Inconsistent Naming**
   - Follow naming convention strictly
   - No spaces, slashes, or special characters

---

## Examples from Codebase

### Good Example: workspace-pr-screenshots.spec.cy.ts

```typescript
describe('Workspace - PR Screenshots', () => {
  it('should capture healthy workspace', () => {
    // Setup state
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [mockAdapterHealthy],
    })

    cy.visit('/app/workspace')

    // Stabilize
    cy.wait('@getAdapters')
    cy.wait(500)

    // Clear naming, descriptive
    cy.screenshot('PR-Screenshot-1-Healthy-Workspace-All-Systems-Operational', {
      overwrite: true,
      capture: 'viewport',
    })
  })
})
```

**Improvement Needed:**

- Rename to: `workspace-healthy-all-operational`
- Copy to: `docs/assets/screenshots/architecture/workspace-healthy-all-operational.png`
- Reference in: `docs/architecture/WORKSPACE_ARCHITECTURE.md`

---

## Review Checklist

When reviewing documentation with screenshots:

- [ ] Screenshot adds value (not redundant with text/diagram)
- [ ] Named according to convention (`{feature}-{state}-{description}`)
- [ ] Stored in correct location (`docs/assets/screenshots/{section}/`)
- [ ] High quality (appropriate viewport, clean state)
- [ ] Alt text is descriptive
- [ ] Caption explains context
- [ ] Referenced in documentation text
- [ ] Test exists to regenerate screenshot
- [ ] No sensitive data visible

---

## Summary

**Key Principles:**

1. **Use sparingly** - Like diagrams, don't overuse (max 1-2 per major section)
2. **Complement diagrams** - Screenshots for UI, diagrams for architecture
3. **Consistent naming** - `{feature}-{state}-{description}.png`
4. **Organized by feature** - `docs/assets/screenshots/{feature-domain}/` for reusability
5. **HD viewport standard** - 1280x720 REQUIRED for all E2E screenshots (Full HD only for exceptions)
6. **Quality standards** - Clean state, realistic data, stable render
7. **Documentation integration** - Always with caption and alt text
8. **Maximize reuse** - Same screenshot can be referenced in multiple documents

**Viewport Quick Reference:**

- **E2E/Documentation: 1280x720 (HD) - REQUIRED**
- Component tests: Flexible based on component size
- Full HD (1920x1080): Exceptional cases only, must justify

**Next Steps:**

1. Review existing screenshots in `cypress/screenshots/`
2. Identify documentation sections that would benefit
3. Create dedicated screenshot tests or enhance existing tests
4. Copy screenshots to `docs/assets/screenshots/`
5. Integrate into documentation with captions

---

**Last Updated:** 2026-02-13
**Owner:** Documentation Team
**Related:** [DOCUMENTATION_ACCEPTANCE_CRITERIA.md](./DOCUMENTATION_ACCEPTANCE_CRITERIA.md)
