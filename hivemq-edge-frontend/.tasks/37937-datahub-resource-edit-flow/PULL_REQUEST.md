# Pull Request: DataHub Resource Management Refactoring

**Kanban Ticket:** https://hivemq.kanbanize.com/ctrl_board/57/cards/37937/details/

## Description

This PR refactors DataHub resource management to separate resource creation/editing from policy node configuration. Users can now manage schemas and scripts directly from the DataHub main page, simplifying both resource management and policy design.

**Key Improvements:**

- **Centralized Resource Management** - Create and edit schemas/scripts from dedicated tables with clear "Create New" and "Edit" actions
- **Simplified Policy Configuration** - Node panels now only select existing resources by name and version, removing complex editing logic
- **Improved User Experience** - Clear separation between "managing resources" (tables) and "using resources" (policy designer)
- **Reduced Complexity** - Node panel components reduced from 200+ lines to simple resource selectors

**User Benefits:**

Users can now manage their schemas and scripts as standalone entities, independent of policy creation. This makes it easier to:

- Create and organize resources before using them in policies
- Edit existing resources without navigating through the policy designer
- Understand the distinction between resource management and policy configuration
- Reuse resources across multiple policies

**Technical Summary:**

- New resource editors using RJSF in side drawers (SchemaEditor, ScriptEditor)
- Simplified node panels (SchemaPanelSimplified, FunctionPanelSimplified) with name/version selectors only
- Resource tables enhanced with Create and Edit actions
- Removed complex conditional logic from SchemaPanel and FunctionPanel
- Publishing flow updated to handle pre-existing resources

---

## BEFORE

**Problem:** Resource management was tightly coupled with policy configuration, creating a confusing user experience.

- Users had to create/edit schemas and scripts from within the policy designer
- Node panels handled both resource CRUD and node configuration in one component
- Complex state management with programmatic update flags to prevent cascading changes
- No way to manage resources independently of policies
- Unclear mental model - "Am I editing a resource or configuring a node?"

---

## AFTER

### 1. Centralized Resource Management

![Schema Table with Create Button]
_Test: cypress/e2e/datahub/resource-edit-flow.spec.cy.ts (1400x1016)_
_Scenario: Users manage schemas directly from the Schemas tab with clear Create New and Edit actions_

**Key Visual Elements:**

- "Create New Schema" button prominently displayed above the table
- Schema table shows all resources with Name, Type, Version columns
- Edit action available on each row for modifying existing schemas
- Clear tabbed interface separating Policies, Schemas, and Scripts

**User Benefits:**
Resources are now first-class citizens in the DataHub UI. Users can create and organize their schemas and scripts before using them in policies, providing a clearer mental model and more flexible workflow.

### 2. Dedicated Resource Editor

![Schema Editor Drawer]
_Test: cypress/e2e/datahub/resource-edit-flow.spec.cy.ts (1400x1016)_
_Scenario: User creates a new JSON schema with Monaco editor for schema definition_

**Key Visual Elements:**

- Side drawer opens with focused resource editing interface
- Name field, Type selector (JSON/Protobuf), and Version display
- Monaco code editor for schema definition with syntax highlighting
- Clear Save/Cancel actions
- Version indicator shows "DRAFT" for new resources, "MODIFIED" when editing

**User Benefits:**
The dedicated editor provides a focused environment for resource creation and editing, with all necessary controls in one place. The Monaco editor offers professional code editing with syntax highlighting and validation.

### 3. Simplified Policy Node Configuration

![Simplified Panel in Designer]
_Test: cypress/e2e/datahub/resource-edit-flow.spec.cy.ts (1400x1016)_
_Scenario: User selects existing schema in policy designer using simplified dropdown interface_

**Key Visual Elements:**

- Simple dropdown to select resource by name
- Version selector showing available versions
- Clean, minimal interface focused on selection only
- No complex editing controls in the panel
- Selected resource immediately reflected in the node

**User Benefits:**
Policy configuration is now straightforward - just select which resource to use. No need to navigate complex forms or manage resource creation while designing policies. The simplified interface reduces cognitive load and focuses user attention on policy logic.

### 4. Consistent Experience Across Resource Types

![Script Table and Editor]
_Test: cypress/e2e/datahub/resource-edit-flow.spec.cy.ts (1400x1016)_
_Scenario: Scripts follow the same management pattern as schemas_

**Key Visual Elements:**

- Scripts tab with identical layout to Schemas tab
- Same Create New / Edit pattern
- Monaco editor for JavaScript code
- Consistent drawer interface

**User Benefits:**
Once users learn how to manage schemas, they already know how to manage scripts. Consistent patterns reduce learning curve and make the interface predictable.

---

## Test Coverage

**Component Tests:**

- ✅ 45 tests for SchemaEditor (creation, editing, validation, Monaco integration)
- ✅ 38 tests for ScriptEditor (creation, editing, versioning)
- ✅ 20 tests for SchemaPanelSimplified (resource selection, version handling)
- ✅ 18 tests for FunctionPanelSimplified (resource selection)
- ✅ **Total: 121 component tests, all passing**

**E2E Tests:**

- ✅ Schema creation and editing flow (JSON and Protobuf)
- ✅ Script creation and editing flow
- ✅ Policy designer integration with resource selection
- ✅ Complete workflow: create schema → use in policy → validate
- ✅ Error handling for missing resource references
- ✅ **Total: 10 E2E scenarios covering full user workflows, all passing**

**Visual Regression:**

- Percy snapshots for all major UI states
- Cypress screenshot comparison for E2E flows

**Accessibility:**

- All new components tested for keyboard navigation
- ARIA labels and roles verified
- Focus management in drawers and panels

---

## Related Issues

- Original Kanban ticket: #37937
- Related architecture documentation: `.tasks/DATAHUB_ARCHITECTURE.md`
- Testing guidelines followed: `.tasks/TESTING_GUIDELINES.md`
