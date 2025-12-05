# Phase 4: E2E Testing Plan - DataHub Resource Edit Flow

**Date:** December 2, 2025  
**Task:** 37937-datahub-resource-edit-flow  
**Status:** 📋 Ready for Execution  
**Duration:** 2-3 days

---

## 🎯 Testing Objectives

### Primary Goals

1. **Verify Complete User Flows** - End-to-end scenarios from resource creation to policy publishing
2. **Test Integration Points** - Ensure new components work together correctly
3. **Validate Against Real Backend** - Use MSW mock database to simulate real API interactions
4. **Catch Regression Issues** - Ensure existing functionality still works
5. **Document Critical Paths** - Create reusable test patterns for future work

### Success Criteria

- ✅ All E2E test scenarios passing
- ✅ No regressions in existing DataHub E2E tests
- ✅ Coverage of all critical user workflows
- ✅ Test execution time < 5 minutes total
- ✅ Clear, maintainable test code following guidelines

---

## 📚 Testing Guidelines Reference

### Must Read BEFORE Writing Tests

1. **AI_AGENT_CYPRESS_COMPLETE_GUIDE.md** - Complete guide for AI agents

   - Running tests correctly (avoid `rm` commands)
   - Using HTML snapshots for debugging
   - Analyzing test failures without screenshots
   - DOM state JSON analysis

2. **CYPRESS_TESTING_GUIDELINES.md** - Comprehensive patterns

   - 6 Critical Rules (MUST follow)
   - Selector strategy (data-testid > ARIA > semantic)
   - Never use `cy.wait()` with arbitrary timeouts
   - Never chain after action commands
   - Always use `--spec` flag for individual tests
   - Never declare completion without running tests

3. **TESTING_GUIDELINES.md** - General testing requirements

   - CRITICAL: Never declare test work complete without running tests
   - Required test commands and output format
   - Accessibility testing patterns
   - Component testing patterns

4. **DATAHUB_ARCHITECTURE.md** - DataHub-specific patterns
   - Node selection required before toolbar actions
   - Policy validation workflow
   - State management (draft store, policy checks store)
   - ToolboxSelectionListener behavior

---

## 🎬 E2E Test Scenarios

### Test File Structure

**Location:** `cypress/e2e/datahub/resource-edit-flow.spec.cy.ts`

**Test Organization:**

```typescript
describe('DataHub - Resource Edit Flow', () => {
  // MSW database setup (follows existing pattern)
  const mswDB: DataHubFactory = factory({
    dataPolicy: { id: primaryKey(String), json: String },
    behaviourPolicy: { id: primaryKey(String), json: String },
    schema: { id: primaryKey(String), json: String },
    script: { id: primaryKey(String), json: String },
  })

  beforeEach(() => {
    drop(mswDB)
    cy_interceptCoreE2E()
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_DATAHUB] })
    cy.intercept('/api/v1/data-hub/function-specs', { items: MOCK_DATAHUB_FUNCTIONS.items })
    cy_interceptDataHubWithMockDB(mswDB)

    loginPage.visit('/app/datahub')
    loginPage.loginButton.click()
    datahubPage.navLink.click()
  })

  describe('Schema Resource Management', () => {
    // Tests for schemas
  })

  describe('Script Resource Management', () => {
    // Tests for scripts
  })

  describe('Policy Designer Integration', () => {
    // Tests for using resources in policies
  })

  describe('Complete Workflow', () => {
    // End-to-end scenarios
  })
})
```

---

## 📝 Test Scenarios (Detailed)

### Scenario 1: Create JSON Schema from Table

**User Story:** As a data architect, I want to create a new JSON schema from the Schema table so that I can define message structures independently.

**Test Steps:**

```typescript
it('should create a new JSON schema from Schema table', () => {
  // 1. Navigate to Schemas tab
  datahubPage.schemasTab.click()

  // 2. Click "Create New Schema" button
  cy.getByTestId('create-schema-button').click()

  // 3. Verify SchemaEditor drawer opens
  cy.getByTestId('schema-editor-drawer').should('be.visible')
  cy.get('h2').should('contain', 'Create New Schema')

  // 4. Fill in schema details
  cy.get('#root_name').type('temperature-reading')
  cy.get('#root_type').select('JSON')

  // 5. Enter JSON schema definition
  const jsonSchema = JSON.stringify(
    {
      type: 'object',
      properties: {
        temperature: { type: 'number' },
        unit: { type: 'string' },
      },
      required: ['temperature'],
    },
    null,
    2
  )

  // Monaco editor interaction
  cy.get('.monaco-editor textarea').first().type(jsonSchema, { force: true })

  // 6. Verify version shows "DRAFT"
  cy.get('#root_version').should('contain', 'DRAFT')

  // 7. Save schema
  cy.intercept('POST', '/api/v1/data-hub/schemas', {
    statusCode: 201,
    body: {
      id: 'temperature-reading',
      version: 1,
      schemaDefinition: jsonSchema,
      type: 'JSON',
    },
  }).as('createSchema')

  cy.getByTestId('save-schema-button').click()
  cy.wait('@createSchema')

  // 8. Verify success toast
  cy.get('.chakra-toast').should('contain', 'Schema Saved')

  // 9. Verify drawer closes
  cy.getByTestId('schema-editor-drawer').should('not.exist')

  // 10. Verify schema appears in table
  cy.getByTestId('schema-table').within(() => {
    cy.contains('td', 'temperature-reading').should('be.visible')
    cy.contains('td', 'JSON').should('be.visible')
    cy.contains('td', '1').should('be.visible')
  })
})
```

**Expected Result:**

- ✅ Drawer opens with empty form
- ✅ All fields editable
- ✅ JSON syntax validated
- ✅ Schema saved successfully
- ✅ Appears in Schema table

---

### Scenario 2: Create Protobuf Schema from Table

**User Story:** As a data architect, I want to create a Protobuf schema to support binary message formats.

**Test Steps:**

```typescript
it('should create a new Protobuf schema from Schema table', () => {
  datahubPage.schemasTab.click()
  cy.getByTestId('create-schema-button').click()

  cy.get('#root_name').type('sensor-data')
  cy.get('#root_type').select('PROTOBUF')

  // Enter Protobuf definition
  const protobufDef = `syntax = "proto3";
message SensorData {
  double temperature = 1;
  string unit = 2;
}`

  cy.get('.monaco-editor textarea').first().type(protobufDef, { force: true })

  // Verify version
  cy.get('#root_version').should('contain', 'DRAFT')

  // Save
  cy.intercept('POST', '/api/v1/data-hub/schemas', {
    statusCode: 201,
    body: {
      id: 'sensor-data',
      version: 1,
      schemaDefinition: protobufDef,
      type: 'PROTOBUF',
    },
  }).as('createSchema')

  cy.getByTestId('save-schema-button').click()
  cy.wait('@createSchema')

  // Verify
  cy.get('.chakra-toast').should('contain', 'Schema Saved')
  cy.getByTestId('schema-table').should('contain', 'sensor-data')
})
```

**Expected Result:**

- ✅ Protobuf editor shows
- ✅ Syntax validated
- ✅ Schema created successfully

---

### Scenario 3: Edit Existing Schema

**User Story:** As a data architect, I want to edit an existing schema to create a new version.

**Test Steps:**

```typescript
it('should edit an existing schema and create new version', () => {
  // Setup: Create schema in mock DB
  const existingSchema = {
    id: 'temperature-reading',
    version: 1,
    schemaDefinition: JSON.stringify({ type: 'object', properties: {} }),
    type: 'JSON',
    createdAt: DateTime.now().toISO(),
  }

  mswDB.schema.create({
    id: existingSchema.id,
    json: JSON.stringify(existingSchema),
  })

  // Navigate to Schemas tab
  datahubPage.schemasTab.click()

  // Click Edit on schema row
  cy.getByTestId('schema-table').within(() => {
    cy.contains('tr', 'temperature-reading').within(() => {
      cy.getByTestId('action-menu-button').click()
    })
  })

  cy.contains('button', 'Edit').click()

  // Verify drawer opens in modify mode
  cy.getByTestId('schema-editor-drawer').should('be.visible')
  cy.get('h2').should('contain', 'Create New Schema Version')

  // Verify name is readonly
  cy.get('#root_name').should('be.disabled')
  cy.get('#root_name').should('have.value', 'temperature-reading')

  // Verify version shows "MODIFIED"
  cy.get('#root_version').should('contain', 'MODIFIED')

  // Modify schema definition
  cy.get('.monaco-editor textarea').first().clear({ force: true })
  cy.get('.monaco-editor textarea')
    .first()
    .type(JSON.stringify({ type: 'object', properties: { temp: { type: 'number' } } }, null, 2), { force: true })

  // Save
  cy.intercept('POST', '/api/v1/data-hub/schemas', {
    statusCode: 201,
    body: { ...existingSchema, version: 2 },
  }).as('updateSchema')

  cy.getByTestId('save-schema-button').click()
  cy.wait('@updateSchema')

  // Verify success
  cy.get('.chakra-toast').should('contain', 'New version of schema')

  // Verify new version in table
  cy.getByTestId('schema-table').should('contain', 'temperature-reading')
  // Note: May need to expand grouped versions to see version 2
})
```

**Expected Result:**

- ✅ Drawer opens with existing data
- ✅ Name field readonly
- ✅ Version shows "MODIFIED"
- ✅ New version created
- ✅ Table shows updated version

---

### Scenario 4: Create JavaScript Script from Table

**User Story:** As a developer, I want to create a transformation script to process message data.

**Test Steps:**

```typescript
it('should create a new JavaScript script from Script table', () => {
  // Navigate to Scripts tab
  datahubPage.scriptsTab.click()

  // Click "Create New Script" button
  cy.getByTestId('create-script-button').click()

  // Verify ScriptEditor drawer opens
  cy.getByTestId('script-editor-drawer').should('be.visible')
  cy.get('h2').should('contain', 'Create New Script')

  // Fill in script details
  cy.get('#root_name').type('temperature-converter')
  cy.get('#root_description').type('Converts Fahrenheit to Celsius')

  // Enter JavaScript code
  const jsCode = `function convert(temp) {
  return (temp - 32) * 5/9;
}`

  cy.get('.monaco-editor textarea').first().type(jsCode, { force: true })

  // Verify version shows "DRAFT"
  cy.get('#root_version').should('contain', 'DRAFT')

  // Save script
  cy.intercept('POST', '/api/v1/data-hub/scripts', {
    statusCode: 201,
    body: {
      id: 'temperature-converter',
      version: 1,
      functionType: 'TRANSFORMATION',
      source: jsCode,
      description: 'Converts Fahrenheit to Celsius',
    },
  }).as('createScript')

  cy.getByTestId('save-script-button').click()
  cy.wait('@createScript')

  // Verify success
  cy.get('.chakra-toast').should('contain', 'Script Saved')
  cy.getByTestId('script-editor-drawer').should('not.exist')

  // Verify script in table
  cy.getByTestId('script-table').within(() => {
    cy.contains('td', 'temperature-converter').should('be.visible')
    cy.contains('td', '1').should('be.visible')
  })
})
```

**Expected Result:**

- ✅ Drawer opens with empty form
- ✅ JavaScript editor active
- ✅ Optional description field
- ✅ Script saved successfully
- ✅ Appears in Script table

---

### Scenario 5: Edit Existing Script

**User Story:** As a developer, I want to update a script to fix bugs or add features.

**Test Steps:**

```typescript
it('should edit an existing script and create new version', () => {
  // Setup: Create script in mock DB
  const existingScript = {
    id: 'temperature-converter',
    version: 1,
    functionType: 'TRANSFORMATION',
    source: 'function convert(temp) { return temp; }',
    description: 'Old description',
    createdAt: DateTime.now().toISO(),
  }

  mswDB.script.create({
    id: existingScript.id,
    json: JSON.stringify(existingScript),
  })

  // Navigate to Scripts tab
  datahubPage.scriptsTab.click()

  // Click Edit on script row
  cy.getByTestId('script-table').within(() => {
    cy.contains('tr', 'temperature-converter').within(() => {
      cy.getByTestId('action-menu-button').click()
    })
  })

  cy.contains('button', 'Edit').click()

  // Verify drawer opens in modify mode
  cy.getByTestId('script-editor-drawer').should('be.visible')
  cy.get('h2').should('contain', 'Create New Script Version')

  // Verify name is readonly
  cy.get('#root_name').should('be.disabled')
  cy.get('#root_name').should('have.value', 'temperature-converter')

  // Verify version shows "MODIFIED"
  cy.get('#root_version').should('contain', 'MODIFIED')

  // Modify script
  cy.get('.monaco-editor textarea').first().clear({ force: true })
  cy.get('.monaco-editor textarea')
    .first()
    .type('function convert(temp) { return (temp - 32) * 5/9; }', { force: true })

  // Update description
  cy.get('#root_description').clear().type('Updated: Converts F to C')

  // Save
  cy.intercept('POST', '/api/v1/data-hub/scripts', {
    statusCode: 201,
    body: { ...existingScript, version: 2, description: 'Updated: Converts F to C' },
  }).as('updateScript')

  cy.getByTestId('save-script-button').click()
  cy.wait('@updateScript')

  // Verify success
  cy.get('.chakra-toast').should('contain', 'New version of script')
})
```

**Expected Result:**

- ✅ Drawer opens with existing data
- ✅ Name readonly, version MODIFIED
- ✅ New version created
- ✅ Description updated

---

### Scenario 6: Use Schema in Policy Designer (Simplified Panel)

**User Story:** As a policy designer, I want to add a schema node to my policy and select an existing schema.

**Test Steps:**

```typescript
it('should select existing schema in policy designer using simplified panel', () => {
  // Setup: Create schema in mock DB
  const schema = {
    id: 'temperature-reading',
    version: 1,
    schemaDefinition: JSON.stringify({ type: 'object' }),
    type: 'JSON',
    createdAt: DateTime.now().toISO(),
  }

  mswDB.schema.create({
    id: schema.id,
    json: JSON.stringify(schema),
  })

  // Create a new policy
  datahubPage.addNewPolicy.click()

  // Add schema node from toolbox
  datahubDesignerPage.toolbox.trigger.click()
  datahubDesignerPage.toolbox.schema.drag('[role="application"][data-testid="rf__wrapper"]')
  datahubDesignerPage.controls.fit.click()

  // Click on schema node to open panel
  datahubDesignerPage.designer.selectNode('SCHEMA')

  // Verify SchemaPanelSimplified opens
  cy.get('[data-testid="panel-container"]').should('be.visible')
  cy.get('h3').should('contain', 'Schema Configuration')

  // Verify name dropdown exists (not creation UI)
  cy.get('#root_name').should('exist')
  cy.get('#root_name').should('not.be.disabled')

  // Select schema by name
  cy.get('#root_name').parent().click()
  cy.get('[role="option"]').contains('temperature-reading').click()

  // Verify version dropdown appears
  cy.get('#root_version').should('be.visible')

  // Version should auto-select latest
  cy.get('#root_version').should('contain', '1 (latest)')

  // Verify readonly preview shows schema content
  cy.get('.monaco-editor').should('be.visible')
  cy.get('.monaco-editor').should('contain.text', '"type": "object"')

  // Save node configuration
  cy.get('button[type="submit"]').click()

  // Verify node displays schema name
  datahubDesignerPage.designer.mode('SCHEMA').should('contain', 'temperature-reading')
})
```

**Expected Result:**

- ✅ Panel shows dropdown (not creation form)
- ✅ Can select existing schema
- ✅ Version auto-selected
- ✅ Readonly preview shown
- ✅ Node updated with selection

---

### Scenario 7: Use Script in Policy Designer (Simplified Panel)

**User Story:** As a policy designer, I want to add a function node to my policy and select an existing script.

**Test Steps:**

```typescript
it('should select existing script in policy designer using simplified panel', () => {
  // Setup: Create script in mock DB
  const script = {
    id: 'temperature-converter',
    version: 1,
    functionType: 'TRANSFORMATION',
    source: 'function convert(temp) { return temp; }',
    createdAt: DateTime.now().toISO(),
  }

  mswDB.script.create({
    id: script.id,
    json: JSON.stringify(script),
  })

  // Create a new policy
  datahubPage.addNewPolicy.click()

  // Add function node from toolbox
  datahubDesignerPage.toolbox.trigger.click()
  datahubDesignerPage.toolbox.function.drag('[role="application"][data-testid="rf__wrapper"]')
  datahubDesignerPage.controls.fit.click()

  // Click on function node to open panel
  datahubDesignerPage.designer.selectNode('FUNCTION')

  // Verify FunctionPanelSimplified opens
  cy.get('[data-testid="panel-container"]').should('be.visible')
  cy.get('h3').should('contain', 'Function Configuration')

  // Select script by name
  cy.get('#root_name').parent().click()
  cy.get('[role="option"]').contains('temperature-converter').click()

  // Verify version dropdown
  cy.get('#root_version').should('be.visible')
  cy.get('#root_version').should('contain', '1 (latest)')

  // Verify readonly preview
  cy.get('.monaco-editor').should('be.visible')
  cy.get('.monaco-editor').should('contain.text', 'function convert')

  // Save
  cy.get('button[type="submit"]').click()

  // Verify node displays script name
  datahubDesignerPage.designer.mode('FUNCTION').should('contain', 'temperature-converter')
})
```

**Expected Result:**

- ✅ Panel shows dropdown (not creation form)
- ✅ Can select existing script
- ✅ Version auto-selected
- ✅ Readonly preview shown
- ✅ Node updated with selection

---

### Scenario 8: Complete Workflow - Create Schema → Use in Policy → Validate

**User Story:** As a user, I want to create a schema and immediately use it in a policy.

**Test Steps:**

```typescript
it('should complete workflow: create schema → use in policy → validate', () => {
  // ===== STEP 1: Create Schema =====
  datahubPage.schemasTab.click()
  cy.getByTestId('create-schema-button').click()

  const schemaName = 'workflow-test-schema'
  const jsonSchema = JSON.stringify({
    type: 'object',
    properties: { data: { type: 'string' } },
  })

  cy.get('#root_name').type(schemaName)
  cy.get('#root_type').select('JSON')
  cy.get('.monaco-editor textarea').first().type(jsonSchema, { force: true })

  cy.intercept('POST', '/api/v1/data-hub/schemas', {
    statusCode: 201,
    body: {
      id: schemaName,
      version: 1,
      schemaDefinition: jsonSchema,
      type: 'JSON',
    },
  }).as('createSchema')

  cy.getByTestId('save-schema-button').click()
  cy.wait('@createSchema')

  // ===== STEP 2: Create Policy and Add Schema Node =====
  datahubPage.policiesTab.click()
  datahubPage.addNewPolicy.click()

  // Add data policy node
  datahubDesignerPage.toolbox.trigger.click()
  datahubDesignerPage.toolbox.dataPolicy.drag('[role="application"]')
  datahubDesignerPage.controls.fit.click()

  // Add schema node
  datahubDesignerPage.toolbox.trigger.click()
  datahubDesignerPage.toolbox.schema.drag('[role="application"]')
  datahubDesignerPage.controls.fit.click()

  // ===== STEP 3: Configure Schema Node =====
  datahubDesignerPage.designer.selectNode('SCHEMA')

  cy.get('#root_name').parent().click()
  cy.get('[role="option"]').contains(schemaName).click()
  cy.get('button[type="submit"]').click()

  // ===== STEP 4: Connect Nodes =====
  datahubDesignerPage.designer.connectNodes('DATA_POLICY', 'onSuccess', 'SCHEMA', 'schema-0')

  // ===== STEP 5: Validate Policy =====
  datahubDesignerPage.designer.selectNode('DATA_POLICY')

  cy.intercept('POST', '/api/v1/data-hub/data-validation/policies/*/dry-run', {
    statusCode: 200,
    body: {
      results: [{ id: 'schema-validation', status: 'SUCCESS' }],
    },
  }).as('dryRun')

  datahubDesignerPage.toolbar.checkPolicy.click()
  cy.wait('@dryRun')

  // Verify validation success
  datahubDesignerPage.toolbar.checkPolicy.should('not.be.disabled')
  cy.get('[data-testid="policy-status-badge"]').should('contain', 'Valid')

  // ===== STEP 6: View Report =====
  datahubDesignerPage.toolbar.showReport.click()
  datahubDesignerPage.dryRunPanel.drawer.should('be.visible')

  // Verify schema reference in report
  cy.get('[data-testid="dry-run-panel"]').should('contain', schemaName)
})
```

**Expected Result:**

- ✅ Schema created successfully
- ✅ Schema available in dropdown
- ✅ Policy validates successfully
- ✅ Report shows schema reference
- ✅ No errors throughout workflow

---

### Scenario 9: Error Handling - Select Non-Existent Resource

**User Story:** As a policy designer, I should see clear errors if a referenced resource is missing.

**Test Steps:**

```typescript
it('should show error when selecting a schema that no longer exists', () => {
  // Setup: Create policy with schema reference
  const policy = {
    id: 'test-policy',
    nodes: [
      {
        id: 'schema-1',
        type: 'SCHEMA',
        data: {
          name: 'deleted-schema',
          version: 1,
        },
      },
    ],
  }

  mswDB.dataPolicy.create({
    id: policy.id,
    json: JSON.stringify(policy),
  })

  // Navigate to policy
  datahubPage.policiesTab.click()
  datahubPage.policiesTable.action(0, 'edit').click()

  // Select schema node
  datahubDesignerPage.designer.selectNode('SCHEMA')

  // Panel should show error (resource not found)
  cy.get('[data-testid="panel-container"]').should('contain', 'Schema not found')

  // OR: Dropdown shows no options
  cy.get('#root_name').parent().click()
  cy.get('[role="option"]').should('not.exist')
  cy.contains('No schemas available').should('be.visible')
})
```

**Expected Result:**

- ✅ Clear error message
- ✅ Guidance to create resource first

---

### Scenario 10: Validation Error - Missing Resource Reference

**User Story:** Policy validation should fail if a schema/script reference is broken.

**Test Steps:**

```typescript
it('should fail validation if referenced schema does not exist', () => {
  // Setup: Create policy with invalid schema reference
  const policy = {
    id: 'test-policy',
    nodes: [
      {
        id: 'data-policy-1',
        type: 'DATA_POLICY',
        data: { id: 'policy-1' },
      },
      {
        id: 'schema-1',
        type: 'SCHEMA',
        data: {
          name: 'non-existent-schema',
          version: 1,
        },
      },
    ],
    edges: [
      {
        source: 'data-policy-1',
        sourceHandle: 'onSuccess',
        target: 'schema-1',
        targetHandle: 'schema-0',
      },
    ],
  }

  mswDB.dataPolicy.create({
    id: policy.id,
    json: JSON.stringify(policy),
  })

  // Navigate to policy
  datahubPage.policiesTab.click()
  datahubPage.policiesTable.action(0, 'edit').click()

  // Select policy node
  datahubDesignerPage.designer.selectNode('DATA_POLICY')

  // Run validation
  cy.intercept('POST', '/api/v1/data-hub/data-validation/policies/*/dry-run', {
    statusCode: 400,
    body: {
      errors: [
        {
          title: 'Schema Not Found',
          detail: 'Schema "non-existent-schema" version 1 does not exist',
        },
      ],
    },
  }).as('dryRun')

  datahubDesignerPage.toolbar.checkPolicy.click()
  cy.wait('@dryRun')

  // Verify error shown
  cy.get('[data-testid="policy-status-badge"]').should('contain', 'Invalid')

  // Open report
  datahubDesignerPage.toolbar.showReport.click()
  datahubDesignerPage.dryRunPanel.drawer.should('be.visible')

  // Verify error details
  cy.get('[data-testid="dry-run-panel"]').should('contain', 'Schema Not Found')
  cy.get('[data-testid="dry-run-panel"]').should('contain', 'non-existent-schema')
})
```

**Expected Result:**

- ✅ Validation fails
- ✅ Clear error message
- ✅ Report shows specific issue

---

## 🔧 Test Implementation Guidelines

### 1. MSW Mock Database Setup

**Pattern (from existing tests):**

```typescript
import { drop, factory, primaryKey } from '@mswjs/data'
import { DateTime } from 'luxon'

const mswDB: DataHubFactory = factory({
  dataPolicy: { id: primaryKey(String), json: String },
  behaviourPolicy: { id: primaryKey(String), json: String },
  schema: { id: primaryKey(String), json: String },
  script: { id: primaryKey(String), json: String },
})

beforeEach(() => {
  drop(mswDB) // Clear database
  // Setup mock data as needed
})
```

### 2. Creating Mock Resources

**Schema:**

```typescript
const mockSchema = {
  id: 'test-schema',
  version: 1,
  schemaDefinition: JSON.stringify({ type: 'object' }),
  type: 'JSON',
  createdAt: DateTime.now().toISO({ format: 'basic' }),
}

mswDB.schema.create({
  id: mockSchema.id,
  json: JSON.stringify(mockSchema),
})
```

**Script:**

```typescript
const mockScript = {
  id: 'test-script',
  version: 1,
  functionType: 'TRANSFORMATION',
  source: 'function test() { return true; }',
  createdAt: DateTime.now().toISO({ format: 'basic' }),
}

mswDB.script.create({
  id: mockScript.id,
  json: JSON.stringify(mockScript),
})
```

### 3. Page Object Usage

**Import:**

```typescript
import { datahubPage, loginPage, datahubDesignerPage } from 'cypress/pages'
```

**Key Page Objects:**

```typescript
// Main DataHub page
datahubPage.navLink.click()
datahubPage.schemasTab.click()
datahubPage.scriptsTab.click()
datahubPage.addNewPolicy.click()

// Designer page
datahubDesignerPage.toolbox.trigger.click()
datahubDesignerPage.toolbox.schema.drag('[role="application"]')
datahubDesignerPage.designer.selectNode('SCHEMA')
datahubDesignerPage.toolbar.checkPolicy.click()
datahubDesignerPage.toolbar.showReport.click()
```

### 4. Monaco Editor Interaction

**Typing into Monaco:**

```typescript
// Force is required because textarea is visually hidden
cy.get('.monaco-editor textarea').first().type('content', { force: true })
```

**Clearing Monaco:**

```typescript
cy.get('.monaco-editor textarea').first().clear({ force: true })
```

### 5. React-Select Interaction

**Opening dropdown:**

```typescript
cy.get('#root_name').parent().click()
```

**Selecting option:**

```typescript
cy.get('[role="option"]').contains('option-text').click()
```

### 6. Intercepts and Waits

**Creating schema:**

```typescript
cy.intercept('POST', '/api/v1/data-hub/schemas', {
  statusCode: 201,
  body: { id: 'schema-id', version: 1 },
}).as('createSchema')

cy.getByTestId('save-button').click()
cy.wait('@createSchema')
```

**Dry-run validation:**

```typescript
cy.intercept('POST', '/api/v1/data-hub/data-validation/policies/*/dry-run', {
  statusCode: 200,
  body: { results: [] },
}).as('dryRun')

datahubDesignerPage.toolbar.checkPolicy.click()
cy.wait('@dryRun')
```

### 7. Critical Rules to Follow

**❌ NEVER:**

- Use `cy.wait(1000)` with arbitrary timeouts
- Chain after action commands: `cy.get('#btn').click().should('exist')`
- Run all tests: `pnpm cypress:run:e2e` (too slow)
- Use `rm` commands (triggers approval)
- Claim tests pass without running them

**✅ ALWAYS:**

- Use `--spec` flag: `pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resource-edit-flow.spec.cy.ts"`
- Wait for network requests: `cy.wait('@createSchema')`
- Re-query after actions: `cy.get('#btn').click()` then `cy.get('#result').should('exist')`
- Use `data-testid` selectors first
- Document test results with actual output

---

## 📊 Test Execution Plan

### Phase 1: Individual Scenario Testing (Day 1)

**Goal:** Write and verify each scenario independently

**Process:**

1. Write Scenario 1 (Create JSON Schema)
2. Run: `pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resource-edit-flow.spec.cy.ts" --grep "should create a new JSON schema"`
3. Fix any failures
4. Document results
5. Repeat for Scenarios 2-5

**Expected Duration:** 4-6 hours

### Phase 2: Integration Scenarios (Day 2 Morning)

**Goal:** Test interactions between components

**Process:**

1. Write Scenarios 6-7 (Policy designer integration)
2. Run scenarios individually
3. Fix any failures
4. Document results

**Expected Duration:** 2-3 hours

### Phase 3: Complete Workflows (Day 2 Afternoon)

**Goal:** End-to-end user journeys

**Process:**

1. Write Scenarios 8-10 (complete workflows + errors)
2. Run scenarios individually
3. Fix any failures
4. Document results

**Expected Duration:** 2-3 hours

### Phase 4: Regression Testing (Day 3)

**Goal:** Ensure no existing functionality broken

**Process:**

1. Run existing DataHub E2E tests:

   ```bash
   pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub.spec.cy.ts"
   pnpm cypress:run:e2e --spec "cypress/e2e/datahub/policy-report.spec.cy.ts"
   ```

2. Fix any regressions

3. Document results

**Expected Duration:** 2-4 hours

### Phase 5: Final Verification (Day 3)

**Goal:** Run all tests together, verify stability

**Process:**

1. Run all DataHub E2E tests:

   ```bash
   pnpm cypress:run:e2e --spec "cypress/e2e/datahub/*.spec.cy.ts"
   ```

2. Verify all pass

3. Create final test report

**Expected Duration:** 1-2 hours

---

## 📝 Test Results Documentation Template

### Format

````markdown
## E2E Test Results - DataHub Resource Edit Flow

**Date:** December X, 2025
**Command:** `pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resource-edit-flow.spec.cy.ts"`
**Duration:** Xs
**Browser:** Electron 138 (headless)

### Summary

- ✅ 10 passing
- ❌ 0 failing
- ⏭️ 0 skipped

### Detailed Results

```
DataHub - Resource Edit Flow
  Schema Resource Management
    ✓ should create a new JSON schema from Schema table (2345ms)
    ✓ should create a new Protobuf schema from Schema table (1876ms)
    ✓ should edit an existing schema and create new version (2123ms)

  Script Resource Management
    ✓ should create a new JavaScript script from Script table (1987ms)
    ✓ should edit an existing script and create new version (2034ms)

  Policy Designer Integration
    ✓ should select existing schema in policy designer using simplified panel (3456ms)
    ✓ should select existing script in policy designer using simplified panel (3234ms)

  Complete Workflow
    ✓ should complete workflow: create schema → use in policy → validate (4567ms)

  Error Handling
    ✓ should show error when selecting a schema that no longer exists (1234ms)
    ✓ should fail validation if referenced schema does not exist (2345ms)

  10 passing (25s)
```

### Screenshots

- `cypress/screenshots/resource-edit-flow/...`

### Videos

- `cypress/videos/resource-edit-flow.spec.cy.ts.mp4`

### Issues Found

- None

### Next Steps

- ✅ All scenarios passing
- ✅ Ready for Phase 4 documentation
````

---

## 🚀 Getting Started

### Prerequisites

1. Read all testing guidelines (listed above)
2. Review existing DataHub E2E tests
3. Understand MSW mock database pattern
4. Familiarize with DataHub page objects

### First Steps

1. Create test file:

   ```bash
   touch cypress/e2e/datahub/resource-edit-flow.spec.cy.ts
   ```

2. Copy test structure template (from above)

3. Start with Scenario 1 (Create JSON Schema)

4. Run individual test:

   ```bash
   pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resource-edit-flow.spec.cy.ts" --grep "should create a new JSON schema"
   ```

5. Iterate until passing

6. Move to next scenario

---

## ✅ Success Criteria

### Test Quality

- ✅ All scenarios pass consistently
- ✅ No flaky tests (pass rate > 95%)
- ✅ Clear test names and descriptions
- ✅ Proper use of page objects
- ✅ No arbitrary waits
- ✅ Proper error handling

### Coverage

- ✅ All user stories covered
- ✅ Happy paths tested
- ✅ Error paths tested
- ✅ Integration points verified
- ✅ Complete workflows validated

### Documentation

- ✅ Test results documented with actual output
- ✅ Issues found and fixed documented
- ✅ Patterns and learnings captured
- ✅ Reusable for future work

### Performance

- ✅ Total execution time < 5 minutes
- ✅ Individual tests < 5 seconds each
- ✅ No timeout issues
- ✅ Stable in CI environment

---

**Next Steps:**

1. Review this plan
2. Read all referenced guidelines
3. Begin Scenario 1 implementation
4. Execute Phase 1 testing plan

**Status:** 📋 Ready for Implementation
