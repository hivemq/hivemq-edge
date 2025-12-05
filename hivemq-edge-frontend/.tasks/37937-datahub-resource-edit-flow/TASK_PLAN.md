# Task 37937: DataHub Resource Edit Flow - Implementation Plan

**Created:** November 26, 2025  
**Task ID:** 37937  
**Status:** 📋 Planning Phase  
**Estimated Duration:** 2-3 weeks  
**Last Updated:** November 26, 2025  
**Version:** 1.1

---

## Executive Summary

This task refactors the DataHub resource management system to separate resource creation/editing from policy node configuration. The current implementation combines both concerns in complex node panels (SchemaPanel/FunctionPanel), leading to maintainability issues and user confusion.

### Key Changes

1. **Resource Management**: Move to main DataHub page (Schema/Script tables)
2. **Node Configuration**: Simplify to resource selection only (name + version dropdowns)
3. **Publishing**: Remove resource publishing from policy publish flow
4. **UX**: Clear separation between "managing resources" and "using resources"

### Implementation Strategy

**4 Phases, 12 Subtasks, Iterative Delivery**

Each phase is independently testable and can be reviewed before proceeding. The approach follows the workspace wizard pattern established in task 38111.

---

## Design Principles

### 1. Separation of Concerns

- Resource CRUD operations are independent from policy design
- Node panels only configure which resource to use
- Clear mental model: "Create resources, then use them in policies"

### 2. Gradual Refactoring

- Build new components alongside existing ones
- Test thoroughly before removing old code
- Maintain backward compatibility throughout

### 3. Reusability

- Common patterns for schemas and scripts
- Shared components where appropriate
- Follow existing DataHub patterns (RJSF, drawers, etc.)

### 4. User Experience

- Consistent with existing DataHub UI
- Clear feedback and validation
- Accessibility-first approach

### 5. Programmatic Testing

**MANDATORY: Every file gets its test suite**

- **Components**: Cypress component test (`.spec.cy.tsx`)
- **Hooks**: Vitest test suite (`.spec.ts`)
- **Utilities**: Vitest test suite (`.spec.ts`)

**Test State Strategy:**

- ✅ **One accessibility test ACTIVE** (allows visual verification)
- ⏭️ **All other tests SKIPPED** (prevents distraction during development)
- 📝 Tests are documented and ready to be activated later

**Example Test File Structure:**

```typescript
describe('ComponentName', () => {
  // ACTIVE - Always run for visual verification
  it('is accessible with keyboard navigation and ARIA labels', () => {
    // Mount with common props
    // Test keyboard navigation
    // Verify ARIA attributes
  })

  // SKIPPED - Will activate during Phase 4
  it.skip('renders correctly with default props', () => {})
  it.skip('handles user interactions', () => {})
  it.skip('shows loading state', () => {})
  it.skip('displays errors appropriately', () => {})
})
```

**Rationale:**

- Ensures test infrastructure exists from day one
- Active accessibility test allows component visualization
- Skipped tests document expected behavior without blocking progress
- Tests can be activated systematically during Phase 4

---

## Phase 1: Resource Editor Infrastructure (Days 1-5)

**Goal**: Build reusable resource editing components that work independently from the policy designer.

### Subtask 1.1: Resource Editor Base Component (Day 1)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Create a reusable drawer component for editing resources (schemas and scripts).

**Tasks**:

1. Create `ResourceEditorDrawer.tsx` base component
   - Generic drawer with RJSF form
   - Props: `isOpen`, `onClose`, `resourceType`, `initialData`, `onSave`
   - Uses existing RJSF patterns from SchemaPanel/FunctionPanel
2. Extract common resource editing logic

   - Form validation
   - Save/cancel handling
   - Error display
   - Loading states

3. Create hook `useResourceEditor.ts`
   - Manages editor state
   - Handles save mutations
   - Form data preparation

**Deliverables**:

- `src/extensions/datahub/components/editors/ResourceEditorDrawer.tsx`
- `src/extensions/datahub/hooks/useResourceEditor.ts`
- `src/extensions/datahub/components/editors/ResourceEditorDrawer.spec.cy.tsx` (with 1 active accessibility test)
- `src/extensions/datahub/hooks/useResourceEditor.spec.ts` (with skipped tests)

**Test Requirements**:

**Component Test** (`ResourceEditorDrawer.spec.cy.tsx`):

```typescript
describe('ResourceEditorDrawer', () => {
  // ✅ ACTIVE - For visual verification
  it('is accessible with keyboard navigation and ARIA labels', () => {
    // Mount with common props (isOpen, mock handlers)
    // Test: Tab navigation through form fields
    // Test: Escape key closes drawer
    // Test: Enter key submits form
    // Verify: ARIA labels, roles, focus management
  })

  // ⏭️ SKIPPED - Document expected behavior
  it.skip('opens and closes drawer correctly', () => {})
  it.skip('renders RJSF form with provided schema', () => {})
  it.skip('calls onSave when form submitted', () => {})
  it.skip('calls onClose when cancelled', () => {})
  it.skip('shows loading state during save', () => {})
  it.skip('displays error message on save failure', () => {})
})
```

**Hook Test** (`useResourceEditor.spec.ts`):

```typescript
describe('useResourceEditor', () => {
  it.skip('manages form state', () => {})
  it.skip('handles save mutation', () => {})
  it.skip('prepares form data correctly', () => {})
  it.skip('handles errors', () => {})
})
```

**Acceptance Criteria**:

- [ ] Component file created and working
- [ ] Hook file created and working
- [ ] Component test file created with 1 active accessibility test
- [ ] Hook test file created with all tests skipped
- [ ] Active accessibility test passes when run:
  ```bash
  pnpm cypress:run:component --spec "src/extensions/datahub/components/editors/ResourceEditorDrawer.spec.cy.tsx"
  ```
- [ ] Component visible and functional in test

---

### Subtask 1.2: Schema Editor Implementation (Day 2)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Implement schema-specific editor using the base component.

**Tasks**:

1. Create `SchemaEditor.tsx`

   - Extends `ResourceEditorDrawer`
   - Schema-specific form (name, type, version, definition)
   - Uses `MOCK_SCHEMA_SCHEMA` JSON-Schema
   - Custom widgets: schema type selector, code editor

2. Implement `useSchemaEditor.ts` hook

   - Loads existing schema data for editing
   - Handles JSON/Protobuf type switching
   - Schema validation (parse JSON, validate Protobuf)
   - Save via `createSchema` / `updateSchema` mutations

3. Form logic:
   - Create new: Generate draft schema structure
   - Edit existing: Load and populate form
   - Version handling: Create new version vs. update existing

**Deliverables**:

- `src/extensions/datahub/components/editors/SchemaEditor.tsx`
- `src/extensions/datahub/hooks/useSchemaEditor.ts`
- `src/extensions/datahub/components/editors/SchemaEditor.spec.cy.tsx` (with 1 active accessibility test)
- `src/extensions/datahub/hooks/useSchemaEditor.spec.ts` (with skipped tests)

**Test Structure**:

**Component Test** (`SchemaEditor.spec.cy.tsx`):

```typescript
describe('SchemaEditor', () => {
  // ✅ ACTIVE - Visual verification
  it('is accessible for creating JSON schema', () => {
    // Mount with create mode props
    // Test keyboard navigation
    // Verify ARIA labels for name, type, definition fields
    // Test type selector accessibility
  })

  // ⏭️ SKIPPED - Ready for Phase 4
  it.skip('creates new JSON schema', () => {})
  it.skip('creates new Protobuf schema', () => {})
  it.skip('edits existing schema', () => {})
  it.skip('validates JSON schema definition', () => {})
  it.skip('validates Protobuf schema definition', () => {})
  it.skip('handles save success', () => {})
  it.skip('handles save error', () => {})
  it.skip('type switching updates form', () => {})
})
```

**Hook Test** (`useSchemaEditor.spec.ts`):

```typescript
describe('useSchemaEditor', () => {
  it.skip('loads existing schema data', () => {})
  it.skip('handles JSON/Protobuf type switching', () => {})
  it.skip('validates JSON schema syntax', () => {})
  it.skip('validates Protobuf schema syntax', () => {})
  it.skip('calls createSchema mutation for new', () => {})
  it.skip('calls updateSchema mutation for edit', () => {})
})
```

**Acceptance Criteria**:

- [ ] SchemaEditor component functional
- [ ] useSchemaEditor hook functional
- [ ] Test files created with proper structure
- [ ] Active accessibility test passes:
  ```bash
  pnpm cypress:run:component --spec "src/extensions/datahub/components/editors/SchemaEditor.spec.cy.tsx"
  ```
- [ ] Component visible in test with proper ARIA labels
- [ ] All other tests documented but skipped

---

### Subtask 1.3: Script Editor Implementation (Day 3)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Implement script-specific editor using the base component.

**Tasks**:

1. Create `ScriptEditor.tsx`

   - Extends `ResourceEditorDrawer`
   - Script-specific form (name, type, version, source code, description)
   - Uses `MOCK_FUNCTION_SCHEMA` JSON-Schema
   - Custom widgets: code editor for JavaScript

2. Implement `useScriptEditor.ts` hook

   - Loads existing script data for editing
   - JavaScript syntax validation
   - Save via `createScript` / `updateScript` mutations

3. Form logic:
   - Create new: Generate draft script structure with template
   - Edit existing: Load and populate form
   - Version handling: Create new version vs. update existing

**Deliverables**:

- `src/extensions/datahub/components/editors/ScriptEditor.tsx`
- `src/extensions/datahub/hooks/useScriptEditor.ts`
- `src/extensions/datahub/components/editors/ScriptEditor.spec.cy.tsx` (with 1 active accessibility test)
- `src/extensions/datahub/hooks/useScriptEditor.spec.ts` (with skipped tests)

**Test Structure**:

**Component Test** (`ScriptEditor.spec.cy.tsx`):

```typescript
describe('ScriptEditor', () => {
  // ✅ ACTIVE - Visual verification
  it('is accessible for creating scripts', () => {
    // Mount with create mode props
    // Test keyboard navigation through fields
    // Verify ARIA labels for name, source code, description
    // Test code editor accessibility
  })

  // ⏭️ SKIPPED - Ready for Phase 4
  it.skip('creates new script', () => {})
  it.skip('edits existing script', () => {})
  it.skip('validates JavaScript syntax', () => {})
  it.skip('handles save success', () => {})
  it.skip('handles save error', () => {})
  it.skip('preserves description', () => {})
  it.skip('code editor has syntax highlighting', () => {})
})
```

**Hook Test** (`useScriptEditor.spec.ts`):

```typescript
describe('useScriptEditor', () => {
  it.skip('loads existing script data', () => {})
  it.skip('validates JavaScript syntax', () => {})
  it.skip('calls createScript mutation for new', () => {})
  it.skip('calls updateScript mutation for edit', () => {})
  it.skip('handles optional description field', () => {})
})
```

**Acceptance Criteria**:

- [ ] ScriptEditor component functional
- [ ] useScriptEditor hook functional
- [ ] Test files created with proper structure
- [ ] Active accessibility test passes:
  ```bash
  pnpm cypress:run:component --spec "src/extensions/datahub/components/editors/ScriptEditor.spec.cy.tsx"
  ```
- [ ] Component visible in test
- [ ] All other tests documented but skipped

---

### Subtask 1.4: Integration with Schema Table (Day 4)

**Duration**: 1 day  
**Complexity**: Low-Medium

**Objective**: Add "Create New" and "Edit" actions to the Schema table.

**Tasks**:

1. Update `SchemaTable.tsx`

   - Add "Create New" button above table
   - Add "Edit" action to `DataHubListAction` dropdown
   - Wire to `SchemaEditor` component
   - Manage drawer open/close state

2. Handle create flow:

   - Click "Create New" → Open `SchemaEditor` with empty form
   - Save → Close drawer, refresh table, show success toast

3. Handle edit flow:

   - Click "Edit" on row → Open `SchemaEditor` with loaded data
   - Save → Close drawer, refresh table, show success toast

4. Update translations:
   - Add keys for "Create New Schema", "Edit Schema"
   - Success/error toast messages

**Deliverables**:

- Updated `SchemaTable.tsx`
- Updated `en/translation.json` (i18n keys)
- Updated `SchemaTable.spec.cy.tsx` (with 1 active accessibility test for new buttons)

**Acceptance Criteria**:

- [ ] "Create New" button visible above table
- [ ] "Edit" action in row dropdown
- [ ] Create flow works end-to-end
- [ ] Edit flow works end-to-end
- [ ] Table refreshes after save
- [ ] Success/error toasts display
- [ ] Accessible button labels
- [ ] Component test covers interactions

**Visual Design**:

```
┌─────────────────────────────────────────────────┐
│ Schemas                                         │
│                                                 │
│ [+ Create New Schema]        [Filter] [Search] │
│                                                 │
│ Table:                                          │
│ ┌──────────────────────────────────────────┐   │
│ │ Name    Type       Version   ...  Actions│   │
│ │ schema1 JSON       1         ...  ⋮      │   │
│ │                                   ├─Edit  │   │
│ │                                   ├─Download│ │
│ │                                   └─Delete│   │
│ └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

---

### Subtask 1.5: Integration with Script Table (Day 5)

**Duration**: 1 day  
**Complexity**: Low-Medium

**Objective**: Add "Create New" and "Edit" actions to the Script table.

**Tasks**:

1. Update `ScriptTable.tsx`

   - Add "Create New" button above table
   - Add "Edit" action to `DataHubListAction` dropdown
   - Wire to `ScriptEditor` component
   - Manage drawer open/close state

2. Handle create flow:

   - Click "Create New" → Open `ScriptEditor` with empty form
   - Save → Close drawer, refresh table, show success toast

3. Handle edit flow:

   - Click "Edit" on row → Open `ScriptEditor` with loaded data
   - Save → Close drawer, refresh table, show success toast

4. Update translations:
   - Add keys for "Create New Script", "Edit Script"
   - Success/error toast messages

**Deliverables**:

- Updated `ScriptTable.tsx`
- Updated `en/translation.json` (i18n keys)
- Updated `ScriptTable.spec.cy.tsx` (with 1 active accessibility test for new buttons)

**Acceptance Criteria**:

- [ ] "Create New" button visible above table
- [ ] "Edit" action in row dropdown
- [ ] Create flow works end-to-end
- [ ] Edit flow works end-to-end
- [ ] Table refreshes after save
- [ ] Success/error toasts display
- [ ] Accessible button labels
- [ ] Component test covers interactions

**Phase 1 Completion Checkpoint**:

- ✅ Users can create schemas from Schema table
- ✅ Users can edit schemas from Schema table
- ✅ Users can create scripts from Script table
- ✅ Users can edit scripts from Script table
- ✅ All operations independent from policy designer
- ✅ Test coverage >80% for new components

---

## Phase 2: Simplified Node Configuration (Days 6-9)

**Goal**: Replace complex node panels with simple resource selection (name + version dropdowns).

### Subtask 2.1: Resource Selection Component (Day 6)

**Duration**: 1 day  
**Complexity**: Low-Medium

**Objective**: Create a reusable component for selecting resources by name and version.

**Tasks**:

1. Create `ResourceSelector.tsx`

   - Two-stage selection: Name dropdown → Version dropdown
   - Props: `resourceType`, `value`, `onChange`, `resources`
   - Displays current selection as badge
   - Uses Chakra UI Select components

2. Implement selection logic:

   - Step 1: Select resource name from all available
   - Step 2: Select version from versions of that resource
   - Load versions dynamically when name changes

3. Visual feedback:
   - Show selected resource info (type, latest version, etc.)
   - Highlight selection changes
   - Clear selection button

**Deliverables**:

- `src/extensions/datahub/components/helpers/ResourceSelector.tsx`
- `src/extensions/datahub/components/helpers/ResourceSelector.spec.cy.tsx` (with 1 active accessibility test)

**Acceptance Criteria**:

- [ ] Name dropdown shows all resources
- [ ] Version dropdown shows versions for selected name
- [ ] Selection persists correctly
- [ ] onChange callback fires with name + version
- [ ] Visual feedback for selection
- [ ] Accessible (keyboard nav, ARIA)
- [ ] Component test passes

**Visual Design**:

```
┌──────────────────────────────────────┐
│ Select Schema                        │
│                                      │
│ Name:    [Dropdown: schema1 ▼]      │
│                                      │
│ Version: [Dropdown: 1, 2, 3 ▼]      │
│                                      │
│ ℹ️ JSON Schema, 3 versions available │
│                                      │
│ Current: schema1 (v2)  [✕ Clear]    │
└──────────────────────────────────────┘
```

---

### Subtask 2.2: Simplified Schema Panel (Day 7)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Replace SchemaPanel with simple resource selection interface.

**Tasks**:

1. Create new `SchemaPanelSimplified.tsx`

   - Uses `ResourceSelector` component
   - Only handles selection (no creation/editing)
   - Loads available schemas via `useGetAllSchemas`
   - Updates node data on selection change

2. Remove complex logic:

   - No programmatic update refs
   - No type switching
   - No schema definition editing
   - No version management (just selection)

3. Keep existing:

   - Guard rails (`usePolicyGuards`)
   - Node update via `onFormSubmit`
   - Error display via `onFormError`

4. Migration strategy:
   - Create new component alongside old
   - Feature flag to switch between implementations
   - Keep old component for reference during testing

**Deliverables**:

- `src/extensions/datahub/designer/schema/SchemaPanelSimplified.tsx`
- `src/extensions/datahub/designer/schema/SchemaPanelSimplified.spec.cy.tsx` (with 1 active accessibility test)
- Feature flag configuration

**Acceptance Criteria**:

- [ ] Panel shows resource selector
- [ ] Can select schema name
- [ ] Can select schema version
- [ ] Selection updates node data
- [ ] Node displays selected resource
- [ ] Guard rails prevent editing when needed
- [ ] LOC reduced by >50% vs. old panel
- [ ] Component test passes

**LOC Comparison**:

- Old `SchemaPanel.tsx`: ~200 lines
- New `SchemaPanelSimplified.tsx`: <100 lines (>50% reduction)

---

### Subtask 2.3: Simplified Function Panel (Day 8)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Replace FunctionPanel with simple resource selection interface.

**Tasks**:

1. Create new `FunctionPanelSimplified.tsx`

   - Uses `ResourceSelector` component
   - Only handles selection (no creation/editing)
   - Loads available scripts via `useGetAllScripts`
   - Updates node data on selection change

2. Remove complex logic:

   - No programmatic update refs
   - No source code editing
   - No version management (just selection)

3. Keep existing:

   - Guard rails (`usePolicyGuards`)
   - Node update via `onFormSubmit`
   - Error display via `onFormError`

4. Migration strategy:
   - Create new component alongside old
   - Feature flag to switch between implementations
   - Keep old component for reference during testing

**Deliverables**:

- `src/extensions/datahub/designer/script/FunctionPanelSimplified.tsx`
- `src/extensions/datahub/designer/script/FunctionPanelSimplified.spec.cy.tsx` (with 1 active accessibility test)
- Feature flag configuration

**Acceptance Criteria**:

- [ ] Panel shows resource selector
- [ ] Can select script name
- [ ] Can select script version
- [ ] Selection updates node data
- [ ] Node displays selected resource
- [ ] Guard rails prevent editing when needed
- [ ] LOC reduced by >50% vs. old panel
- [ ] Component test passes

---

### Subtask 2.4: Node Display Updates (Day 9)

**Duration**: 1 day  
**Complexity**: Low

**Objective**: Update SchemaNode and FunctionNode to display selected resource clearly.

**Tasks**:

1. Update `SchemaNode.tsx`

   - Display: resource name + version
   - Visual indicator if resource exists vs. missing
   - Tooltip with resource details (type, description)

2. Update `FunctionNode.tsx`

   - Display: resource name + version
   - Visual indicator if resource exists vs. missing
   - Tooltip with resource details (function type, description)

3. Visual design:
   - Use existing `NodeParams` component
   - Add badge for version
   - Error state if resource not found

**Deliverables**:

- Updated `SchemaNode.tsx`
- Updated `FunctionNode.tsx`
- Component tests

**Acceptance Criteria**:

- [ ] Node shows resource name clearly
- [ ] Node shows version as badge
- [ ] Tooltip shows resource details on hover
- [ ] Error indicator if resource missing
- [ ] Consistent with existing node styling
- [ ] Accessible (ARIA labels)
- [ ] Component tests pass

**Visual Design**:

```
┌──────────────────────────┐
│ 📄 Schema Node          │
│                          │
│ schema1       v2  ✓     │
│ (JSON)                   │
└──────────────────────────┘

┌──────────────────────────┐
│ ⚡ Function Node         │
│                          │
│ transform    v1  ✓      │
│ (JavaScript)             │
└──────────────────────────┘

Missing resource:
┌──────────────────────────┐
│ 📄 Schema Node          │
│                          │
│ deleted_schema  v1  ⚠️  │
│ (Resource not found)     │
└──────────────────────────┘
```

**Phase 2 Completion Checkpoint**:

- ✅ SchemaPanel simplified to <100 LOC
- ✅ FunctionPanel simplified to <100 LOC
- ✅ Nodes display selected resources clearly
- ✅ No resource creation in node panels
- ✅ Test coverage >80% for new components

---

## Phase 3: Publishing Flow Updates (Days 10-12)

**Goal**: Update policy validation and publishing to work with resource references.

### Subtask 3.1: Dry-Run Validation Updates (Day 10)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Update dry-run validation to check resource references exist.

**Tasks**:

1. Update `usePolicyDryRun.ts`

   - Extract resource references from schema/function nodes
   - Validate each reference exists (query resource by name + version)
   - Add validation errors for missing resources
   - Include resource info in dry-run report

2. Validation logic:

   ```typescript
   // For each schema node
   - Check if schema with name + version exists
   - If not, add error: "Schema 'X' version Y not found"

   // For each function node
   - Check if script with name + version exists
   - If not, add error: "Script 'X' version Y not found"
   ```

3. Update error reporting:
   - Clear error messages for missing resources
   - Link to resource management page
   - Suggest creating missing resource

**Deliverables**:

- Updated `usePolicyDryRun.ts`
- Unit tests for validation logic

**Acceptance Criteria**:

- [ ] Validates all schema references exist
- [ ] Validates all script references exist
- [ ] Errors added to report for missing resources
- [ ] Error messages are clear and actionable
- [ ] Dry-run report includes resource info
- [ ] Unit tests pass with >80% coverage

**Error Message Examples**:

```
❌ Schema Reference Error
Schema "temperature_schema" version 2 not found.
This schema is used by the validator node but doesn't exist.
→ Create this schema from the Schema table

❌ Script Reference Error
Script "transform_data" version 1 not found.
This script is used by the function node but doesn't exist.
→ Create this script from the Script table
```

---

### Subtask 3.2: Publishing Logic Refactor (Day 11)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Remove resource publishing from ToolbarPublish, focus only on policy publishing.

**Tasks**:

1. Update `ToolbarPublish.tsx`

   - Remove `publishResources()` function
   - Remove resource reducer logic
   - Remove schema/script creation mutations
   - Simplify to only publish policy

2. New publishing flow:

   ```typescript
   // OLD FLOW
   1. Extract resources from dry-run report
   2. Publish schemas (createSchema mutations)
   3. Publish scripts (createScript mutations)
   4. Publish policy (createPolicy/updatePolicy)

   // NEW FLOW
   1. Validate resources exist (already done in dry-run)
   2. Publish policy (createPolicy/updatePolicy)
   3. Policy references resources by name + version
   ```

3. Update policy payload:

   - Ensure resource references use name + version (not embedded data)
   - Backend validates resource existence

4. Update success/error handling:
   - Success: "Policy published successfully"
   - Error: Clear message if resource validation fails server-side

**Deliverables**:

- Updated `ToolbarPublish.tsx`
- Unit tests

**Acceptance Criteria**:

- [ ] No resource publishing in policy publish flow
- [ ] Policy published with resource references
- [ ] Success toast shows policy name
- [ ] Error handling for missing resources
- [ ] LOC reduced vs. old implementation
- [ ] Unit tests pass

**LOC Comparison**:

- Old `ToolbarPublish.tsx`: ~250 lines
- New `ToolbarPublish.tsx`: ~150 lines (40% reduction)

---

### Subtask 3.3: PolicySummaryReport Updates (Day 12)

**Duration**: 1 day  
**Complexity**: Low-Medium

**Objective**: Update success summary to reflect new publishing flow (no resources published).

**Tasks**:

1. Update `PolicySummaryReport.tsx`

   - Remove resource creation/update messaging
   - Focus on policy published successfully
   - Show resource references (not resource creation)

2. Success summary content:

   - Policy name + type
   - Resource references used (name + version)
   - "All resources already published" message
   - Link to resource management if needed

3. Update dry-run panel:
   - Show resource reference validation results
   - Highlight missing resources prominently

**Deliverables**:

- Updated `PolicySummaryReport.tsx`
- Updated `DryRunPanelController.tsx` if needed
- Component tests

**Acceptance Criteria**:

- [ ] Success message focuses on policy
- [ ] Resource references displayed clearly
- [ ] No "resources created" messaging
- [ ] Missing resources highlighted in validation
- [ ] Component tests pass

**Visual Design**:

```
✅ Policy Validation Successful

Policy: "temperature_monitoring" (Data Policy)

Resources Used:
  📄 temperature_schema (v2) - JSON
  ⚡ validate_temp (v1) - JavaScript

All resources are published and available.

[Publish Policy]  [Close]
```

**Phase 3 Completion Checkpoint**:

- ✅ Dry-run validates resource references
- ✅ Publishing only publishes policy
- ✅ No resource publishing in policy flow
- ✅ Clear error messages for missing resources
- ✅ Success summary reflects new flow

---

## Phase 4: Testing & Documentation (Days 13-15)

**Goal**: Comprehensive testing and documentation for the refactored system.

### Subtask 4.1: Component Test Coverage (Day 13)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Achieve >80% test coverage for all new and modified components.

**Tasks**:

1. Component tests for new editors:

   - `ResourceEditorDrawer.spec.cy.tsx`
   - `SchemaEditor.spec.cy.tsx`
   - `ScriptEditor.spec.cy.tsx`

2. Component tests for new panels:

   - `SchemaPanelSimplified.spec.cy.tsx`
   - `FunctionPanelSimplified.spec.cy.tsx`
   - `ResourceSelector.spec.cy.tsx`

3. Component tests for updated components:

   - `SchemaTable.spec.cy.tsx` (updated)
   - `ScriptTable.spec.cy.tsx` (updated)
   - `SchemaNode.spec.cy.tsx` (updated)
   - `FunctionNode.spec.cy.tsx` (updated)

4. Test coverage requirements:
   - All user interactions
   - Accessibility (keyboard nav, screen readers)
   - Error states
   - Loading states
   - Edge cases

**Deliverables**:

- 10+ component test files
- Coverage report showing >80%

**Acceptance Criteria**:

- [ ] All new components have tests
- [ ] All modified components have updated tests
- [ ] Coverage >80% for affected code
- [ ] All accessibility tests pass
- [ ] All tests pass in CI/CD

---

### Subtask 4.2: E2E Test Scenarios (Day 14)

**Duration**: 1 day  
**Complexity**: Medium

**Objective**: Create end-to-end tests for complete resource → policy flows.

**Tasks**:

1. E2E test: Create schema and use in policy

   ```typescript
   it('creates schema from table and uses in policy', () => {
     // 1. Navigate to DataHub
     // 2. Click "Create New" on Schema table
     // 3. Fill schema form and save
     // 4. Navigate to policy designer
     // 5. Add schema node
     // 6. Select created schema
     // 7. Validate and publish policy
   })
   ```

2. E2E test: Edit schema and update policy

   ```typescript
   it('edits existing schema and updates policy', () => {
     // 1. Navigate to Schema table
     // 2. Click "Edit" on schema row
     // 3. Modify schema definition
     // 4. Save as new version
     // 5. Navigate to policy using old version
     // 6. Update schema node to new version
     // 7. Validate and publish
   })
   ```

3. E2E test: Missing resource error handling

   ```typescript
   it('shows error when resource reference missing', () => {
     // 1. Create policy with schema reference
     // 2. Delete the schema
     // 3. Open policy designer
     // 4. Run dry-run validation
     // 5. Expect error about missing schema
     // 6. Verify cannot publish
   })
   ```

4. Create Page Objects:
   - `SchemaEditorPage.ts`
   - `ScriptEditorPage.ts`
   - Update `DesignerPage.ts` for new panels

**Deliverables**:

- 6-8 E2E test scenarios
- Page Objects for new components
- E2E test documentation

**Acceptance Criteria**:

- [ ] Complete resource creation flow tested
- [ ] Complete policy usage flow tested
- [ ] Error handling scenarios tested
- [ ] Backward compatibility tested
- [ ] All E2E tests pass consistently
- [ ] Page Objects documented

---

### Subtask 4.3: Documentation & Migration Guide (Day 15)

**Duration**: 1 day  
**Complexity**: Low

**Objective**: Document the new system and provide migration guidance.

**Tasks**:

1. Update DATAHUB_ARCHITECTURE.md

   - New resource management section
   - Updated publishing flow diagram
   - New component architecture
   - State management updates

2. Create RESOURCE_MANAGEMENT.md

   - User guide for creating/editing resources
   - Best practices for resource organization
   - Version management strategy
   - Troubleshooting common issues

3. Create MIGRATION_GUIDE.md

   - Changes from old to new system
   - Impact on existing policies
   - How to adapt workflows
   - FAQ section

4. Update inline code documentation:

   - JSDoc comments for new components
   - Type definitions with descriptions
   - Example usage in comments

5. Create demo video/GIF:
   - Creating a schema from table
   - Using schema in policy
   - Editing existing resource

**Deliverables**:

- Updated `.tasks/DATAHUB_ARCHITECTURE.md`
- New `.tasks/37937-datahub-resource-edit-flow/RESOURCE_MANAGEMENT.md`
- New `.tasks/37937-datahub-resource-edit-flow/MIGRATION_GUIDE.md`
- Inline documentation in code
- Demo assets (video/GIF)

**Acceptance Criteria**:

- [ ] Architecture docs updated
- [ ] User guide complete
- [ ] Migration guide complete
- [ ] Code comments comprehensive
- [ ] Demo assets created
- [ ] Documentation reviewed

**Phase 4 Completion Checkpoint**:

- ✅ Test coverage >80% for all new/modified code
- ✅ E2E tests cover complete flows
- ✅ Documentation complete and reviewed
- ✅ Migration guide available for users
- ✅ Ready for PR submission

---

## Rollout Strategy

### Feature Flag Approach

Implement with feature flag to allow gradual rollout:

```typescript
// config/features.ts
export const FEATURES = {
  DATAHUB_SIMPLIFIED_RESOURCES: import.meta.env.VITE_FEATURE_SIMPLIFIED_RESOURCES === 'true'
}

// Usage in code
{FEATURES.DATAHUB_SIMPLIFIED_RESOURCES ? (
  <SchemaPanelSimplified {...props} />
) : (
  <SchemaPanel {...props} />
)}
```

### Rollout Phases

**Phase A: Development (Week 1-2)**

- Feature flag OFF by default
- Enable locally for development
- Build all components
- Internal testing

**Phase B: Internal Testing (Week 3)**

- Feature flag ON for dev environment
- Team testing and feedback
- Bug fixes and refinements
- Performance validation

**Phase C: Beta Release (Week 4)**

- Feature flag ON for staging
- Selected users test new flow
- Collect feedback and metrics
- Address issues

**Phase D: Full Release (Week 5)**

- Feature flag ON for production
- Monitor usage and errors
- Collect user feedback
- Plan old code removal (Week 6)

**Phase E: Cleanup (Week 6+)**

- Remove old components
- Remove feature flag
- Final documentation update

---

## Success Metrics

### Quantitative Metrics

1. **Code Complexity Reduction**

   - SchemaPanel: 200 LOC → <100 LOC (>50% reduction) ✅
   - FunctionPanel: 200 LOC → <100 LOC (>50% reduction) ✅
   - ToolbarPublish: 250 LOC → ~150 LOC (40% reduction) ✅

2. **Test Coverage**

   - New components: >80% coverage ✅
   - Modified components: >80% coverage ✅
   - E2E coverage: All critical paths ✅

3. **Performance**
   - Resource editor load: <500ms ✅
   - Node panel load: <200ms ✅
   - No regression in designer performance ✅

### Qualitative Metrics

1. **Code Maintainability**

   - Clearer separation of concerns ✅
   - Reduced coupling between components ✅
   - Easier to understand and modify ✅

2. **User Experience**

   - Clear mental model (manage resources, then use them) ✅
   - Consistent UI patterns ✅
   - Better error messages ✅

3. **Developer Experience**
   - Easier to add new resource types ✅
   - Simpler debugging ✅
   - Faster onboarding for new developers ✅

---

## Risk Management

### Technical Risks

**Risk 1: Backward Compatibility Issues**

- **Probability**: Medium
- **Impact**: High
- **Mitigation**:
  - Comprehensive E2E tests for existing policies
  - Feature flag for gradual rollout
  - Data migration if needed
  - Keep old code during transition

**Risk 2: Complex State Migration**

- **Probability**: Low
- **Impact**: Medium
- **Mitigation**:
  - Reuse existing Zustand stores
  - Add new stores only if necessary
  - Thorough testing of state transitions
  - Clear documentation of state flow

**Risk 3: API Contract Changes**

- **Probability**: Low
- **Impact**: High
- **Mitigation**:
  - Verify API contracts early
  - Coordinate with backend team
  - Plan API version migration if needed
  - Test with real backend

### User Experience Risks

**Risk 4: User Confusion During Transition**

- **Probability**: High
- **Impact**: Medium
- **Mitigation**:
  - Clear migration guide
  - In-app tooltips and guidance
  - Beta testing with key users
  - Support documentation and videos

**Risk 5: Workflow Disruption**

- **Probability**: Medium
- **Impact**: Medium
- **Mitigation**:
  - Maintain feature parity
  - Provide alternative paths for workflows
  - Collect user feedback early
  - Iterate based on feedback

---

## Dependencies & Prerequisites

### Internal Dependencies

- None (self-contained refactoring)

### External Dependencies

- Existing API endpoints for resource CRUD
- Existing RJSF infrastructure
- Existing Zustand stores
- Existing Chakra UI components

### Prerequisites

- Understanding of DATAHUB_ARCHITECTURE.md ✅
- Understanding of current SchemaPanel/FunctionPanel implementation ✅
- Access to DataHub API documentation
- Test environment with DataHub backend

---

## Timeline & Milestones

### Week 1: Resource Management Infrastructure

- **Days 1-5**: Phase 1 complete
- **Milestone**: Can create/edit resources from tables ✅

### Week 2: Simplified Node Configuration

- **Days 6-9**: Phase 2 complete
- **Milestone**: Node panels simplified to resource selection ✅

### Week 3: Publishing & Testing

- **Days 10-12**: Phase 3 complete
- **Days 13-15**: Phase 4 complete
- **Milestone**: Ready for PR submission ✅

### Week 4+: Rollout & Refinement

- Beta testing and feedback collection
- Bug fixes and polish
- Documentation updates
- Full production release

---

## Subtask Checklist

### Phase 1: Resource Editor Infrastructure

- [ ] 1.1: Resource Editor Base Component
- [ ] 1.2: Schema Editor Implementation
- [ ] 1.3: Script Editor Implementation
- [ ] 1.4: Integration with Schema Table
- [ ] 1.5: Integration with Script Table

### Phase 2: Simplified Node Configuration

- [ ] 2.1: Resource Selection Component
- [ ] 2.2: Simplified Schema Panel
- [ ] 2.3: Simplified Function Panel
- [ ] 2.4: Node Display Updates

### Phase 3: Publishing Flow Updates

- [ ] 3.1: Dry-Run Validation Updates
- [ ] 3.2: Publishing Logic Refactor
- [ ] 3.3: PolicySummaryReport Updates

### Phase 4: Testing & Documentation

- [ ] 4.1: Component Test Coverage
- [ ] 4.2: E2E Test Scenarios
- [ ] 4.3: Documentation & Migration Guide

---

## Next Steps

**Immediate Actions:**

1. Review and approve this task plan
2. Set up feature flag configuration
3. Create initial branch: `feature/37937-datahub-resource-edit-flow`
4. Begin Subtask 1.1: Resource Editor Base Component

**Before Starting Development:**

- [ ] TASK_PLAN.md reviewed and approved
- [ ] DATAHUB_ARCHITECTURE.md reviewed
- [ ] Current implementation analysis complete
- [ ] Test environment ready
- [ ] Team notified of upcoming changes

---

**Document Version**: 1.1  
**Created**: November 26, 2025  
**Last Updated**: November 26, 2025  
**Status**: Ready for Review

**Version History:**

- v1.0 (Nov 26, 2025): Initial plan with 12 subtasks, 4 phases
- v1.1 (Nov 26, 2025): Added programmatic testing approach (1 active accessibility test + skipped tests per file)
