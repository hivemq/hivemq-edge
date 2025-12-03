import { drop, factory, primaryKey } from '@mswjs/data'
import { DateTime } from 'luxon'

import type { DataHubFactory } from 'cypress/utils/intercept.utils.ts'
import { cy_interceptCoreE2E, cy_interceptDataHubWithMockDB } from 'cypress/utils/intercept.utils.ts'
import { datahubPage, loginPage, datahubDesignerPage, monacoEditor } from 'cypress/pages'

import type { PolicySchema, DataPolicy } from '@/api/__generated__'
import { DataPolicyValidator, Script } from '@/api/__generated__'
import { MOCK_CAPABILITY_DATAHUB } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { MOCK_SCHEMA_SOURCE } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

describe('DataHub - Resource Edit Flow', () => {
  // Creating a mock storage for the Data Hub
  const mswDB: DataHubFactory = factory({
    dataPolicy: {
      id: primaryKey(String),
      json: String,
    },
    behaviourPolicy: {
      id: primaryKey(String),
      json: String,
    },
    schema: {
      id: primaryKey(String),
      json: String,
    },
    script: {
      id: primaryKey(String),
      json: String,
    },
  })

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_DATAHUB] })
    cy.intercept('/api/v1/data-hub/function-specs', {
      items: MOCK_DATAHUB_FUNCTIONS.items.map((specs) => {
        specs.metadata.inLicenseAllowed = true
        return specs
      }),
    })
    cy_interceptDataHubWithMockDB(mswDB)

    loginPage.visit('/app/datahub')
    loginPage.loginButton.click()
    datahubPage.navLink.click()

    // TODO: Create a custom Cypress command to handle this
    // Ignore Monaco worker loading errors and cancellation errors
    cy.on('uncaught:exception', (err: Error, run, promise) => {
      if (promise) {
        // must be { msg: "operation is manually canceled", type: "cancelation" }
        return false
      }
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  describe('Schema Resource Management', () => {
    beforeEach(() => {
      // Ignore Monaco worker loading errors and React Query cancelation errors
      // Cypress.on('uncaught:exception', () => false)

      // Intercept schema POST to add to mock DB automatically
      cy.intercept('POST', '/api/v1/data-hub/schemas', (req) => {
        const schema = req.body
        const response = {
          ...schema,
          version: schema.version || 1,
          createdAt: schema.createdAt || DateTime.now().toISO({ format: 'basic' }),
        }
        req.reply({ statusCode: 201, body: response })

        // Add or update in mock database (handles both create and edit)
        const existing = mswDB.schema.findFirst({ where: { id: { equals: response.id } } })
        if (existing) {
          mswDB.schema.update({
            where: { id: { equals: response.id } },
            data: { json: JSON.stringify(response) },
          })
        } else {
          mswDB.schema.create({
            id: response.id,
            json: JSON.stringify(response),
          })
        }
      }).as('saveSchema')
    })

    it('should create a new JSON schema from Schema table', () => {
      // NARRATIVE: User wants to create a new JSON schema for temperature readings
      // They navigate to schemas, click create, fill in details, and save

      datahubPage.schemasTab.click()

      // SCREENSHOT 1: Schema Table with Create Button
      cy.percySnapshot('Schema Table - Create Button Visible')
      cy.screenshot('01-schema-table-with-create-button')

      datahubPage.schemasTable.createButton.click()

      // Verify SchemaEditor drawer opens in create mode
      datahubPage.schemaEditor.drawer.should('be.visible')
      datahubPage.schemaEditor.title.should('contain', 'Create New Schema')

      // Fill in schema name
      datahubPage.schemaEditor.nameField.type('temperature-reading')

      // Select schema type using POM method
      datahubPage.schemaEditor.selectType('JSON')

      // Verify version shows "DRAFT" for new schema
      datahubPage.schemaEditor.versionDisplay.should('contain.text', 'DRAFT')

      // Enter JSON schema definition using Monaco API
      const jsonSchema = JSON.stringify(MOCK_SCHEMA_SOURCE, null, 2)

      monacoEditor.setValue('#root_schemaSource', jsonSchema)

      // SCREENSHOT 2: Schema Editor Drawer - Filled Form
      cy.percySnapshot('Schema Editor - JSON Schema Creation')
      cy.screenshot('02-schema-editor-drawer-filled')

      // Save (intercept handled in beforeEach)
      datahubPage.schemaEditor.saveButton.click()
      cy.wait('@saveSchema')

      // Verify success toast
      datahubPage.toast.shouldContain('Schema')

      // Verify drawer closed
      datahubPage.schemaEditor.drawer.should('not.exist')

      // Verify new schema appears in the table with correct details
      datahubPage.schemasTable.cell(0, 'name').should('contain.text', 'temperature-reading')
      datahubPage.schemasTable.cell(0, 'type').should('contain.text', 'JSON')
      datahubPage.schemasTable.cell(0, 'version').should('contain.text', '1')
    })

    it('should create a new Protobuf schema from Schema table', () => {
      // NARRATIVE: User creates a Protobuf schema for binary message formats
      // Process is similar to JSON schema but with different type selection

      datahubPage.schemasTab.click()
      datahubPage.schemasTable.createButton.click()

      datahubPage.schemaEditor.drawer.should('be.visible')
      datahubPage.schemaEditor.title.should('contain', 'Create New Schema')

      datahubPage.schemaEditor.nameField.type('sensor-data')

      // Select PROTOBUF type using POM method
      datahubPage.schemaEditor.selectType('PROTOBUF')

      // Verify version shows DRAFT
      datahubPage.schemaEditor.versionDisplay.should('contain.text', 'DRAFT')

      // Enter Protobuf definition using Monaco API
      const protobufDef = `syntax = "proto3";\nmessage SensorData {\n  double temperature = 1;\n  string unit = 2;\n}`
      monacoEditor.setValue('#root_schemaSource', protobufDef)

      // Select messageType using POM method (waits for field, scrolls, and selects)
      datahubPage.schemaEditor.selectMessageType('SensorData')

      // Save button should now be enabled
      datahubPage.schemaEditor.saveButton.should('not.be.disabled')

      // Save (intercept handled in beforeEach)
      datahubPage.schemaEditor.saveButton.click()
      cy.wait('@saveSchema')

      // Verify success via toast
      datahubPage.toast.shouldContain('Schema')
      // Verify drawer closed
      datahubPage.schemaEditor.drawer.should('not.exist')

      // Verify schema appears in table
      datahubPage.schemasTable.cell(0, 'name').should('contain.text', 'sensor-data')
      datahubPage.schemasTable.cell(0, 'type').should('contain.text', 'PROTOBUF')
    })

    describe('editing existing resources', () => {
      beforeEach(function () {
        // Create existing schema BEFORE tab click
        const existingSchema = {
          id: 'temperature-reading',
          version: 1,
          schemaDefinition: btoa(JSON.stringify({ type: 'object', properties: {} })),
          type: 'JSON',
          createdAt: DateTime.now().toISO({ format: 'basic' }),
        }

        mswDB.schema.create({
          id: existingSchema.id,
          json: JSON.stringify(existingSchema),
        })

        // Store for use in test
        this.existingSchema = existingSchema
      })

      it('should edit an existing schema and create new version', function () {
        // NARRATIVE: User needs to update an existing schema definition
        // Editing creates a new version - name is locked but content can be modified

        // Navigate to schemas tab (data already created in beforeEach)
        datahubPage.schemasTab.click()

        // Wait for schema to appear in table
        datahubPage.schemasTable.cell(0, 'name').should('contain.text', 'temperature-reading')

        // Click edit action
        datahubPage.schemasTable.action(0, 'edit').click()

        // Verify editor opens in modify mode
        datahubPage.schemaEditor.drawer.should('be.visible')
        datahubPage.schemaEditor.title.should('contain', 'Create New Schema Version')

        // Verify name is readonly (cannot change ID when creating new version)
        datahubPage.schemaEditor.nameField.should('have.attr', 'readonly')
        datahubPage.schemaEditor.nameField.should('have.value', 'temperature-reading')

        // Verify version shows MODIFIED (not DRAFT, since editing existing)
        datahubPage.schemaEditor.versionDisplay.should('contain.text', 'MODIFIED')

        // Modify the schema definition using Monaco API
        const updatedSchema = JSON.stringify({ type: 'object', properties: { temp: { type: 'number' } } }, null, 2)
        monacoEditor.setValue('#root_schemaSource', updatedSchema)

        // Save (intercept in beforeEach handles this)
        datahubPage.schemaEditor.saveButton.click()
        cy.wait('@saveSchema')

        // Verify success toast mentions version creation
        datahubPage.toast.shouldContain('version')

        // Verify drawer closed
        datahubPage.schemaEditor.drawer.should('not.exist')

        // Verify schema still appears (now with version 2, but table may show aggregated view)
        datahubPage.schemasTable.cell(0, 'name').should('contain.text', 'temperature-reading')
      })
    })
  })

  describe('Script Resource Management', () => {
    beforeEach(() => {
      // Ignore Monaco worker loading errors and React Query cancelation errors
      // Cypress.on('uncaught:exception', () => false)

      // Intercept script POST to add to mock DB automatically
      cy.intercept('POST', '/api/v1/data-hub/scripts', (req) => {
        const script = req.body
        const response = {
          ...script,
          version: script.version || 1,
          createdAt: script.createdAt || DateTime.now().toISO({ format: 'basic' }),
        }
        req.reply({ statusCode: 201, body: response })

        // Add or update in mock database (handles both create and edit)
        const existing = mswDB.script.findFirst({ where: { id: { equals: response.id } } })
        if (existing) {
          mswDB.script.update({
            where: { id: { equals: response.id } },
            data: { json: JSON.stringify(response) },
          })
        } else {
          mswDB.script.create({
            id: response.id,
            json: JSON.stringify(response),
          })
        }
      }).as('saveScript')
    })

    it('should create a new JavaScript script from Script table', () => {
      // NARRATIVE: User wants to create a transformation script for temperature conversion
      // They navigate to scripts, create new, provide name/description, write code, and save

      datahubPage.scriptsTab.click()

      // SCREENSHOT 4: Script Table with Create Button (consistency across resource types)
      cy.percySnapshot('Script Table - Create Button Visible')
      cy.screenshot('04-script-table-with-create-button')

      datahubPage.scriptsTable.createButton.click()

      datahubPage.scriptEditor.drawer.should('be.visible')
      datahubPage.scriptEditor.title.should('contain', 'Create New Script')

      datahubPage.scriptEditor.nameField.type('temperature-converter')
      datahubPage.scriptEditor.descriptionField.type('Converts Fahrenheit to Celsius')

      // Verify version shows DRAFT for new script
      datahubPage.scriptEditor.versionDisplay.should('contain.text', 'DRAFT')

      // Write JavaScript code using Monaco API
      const jsCode = `function convert(temp) {\n  return (temp - 32) * 5/9;\n}`
      monacoEditor.setValue('#root_sourceCode', jsCode)

      // Save (intercept handled in beforeEach)
      datahubPage.scriptEditor.saveButton.click()
      cy.wait('@saveScript')

      // Verify success toast
      datahubPage.toast.shouldContain('Script')

      // Verify drawer closed
      datahubPage.scriptEditor.drawer.should('not.exist')

      // Verify script appears in table
      datahubPage.scriptsTable.rows.should('have.length', 1)
      datahubPage.scriptsTable.cell(0, 'name').should('contain.text', 'temperature-converter')
      datahubPage.scriptsTable.cell(0, 'version').should('contain.text', '1')
    })

    describe('editing existing resources', () => {
      beforeEach(function () {
        // Create existing script BEFORE tab click
        const existingScript: Script = {
          id: 'temperature-converter',
          version: 1,
          functionType: Script.functionType.TRANSFORMATION,
          source: btoa('function convert(temp) { return temp; }'),
          description: 'Old description',
          createdAt: DateTime.now().toISO({ format: 'basic' }),
        }

        mswDB.script.create({
          id: existingScript.id,
          json: JSON.stringify(existingScript),
        })

        // Store for use in test
        this.existingScript = existingScript
      })

      it('should edit an existing script and create new version', function () {
        // NARRATIVE: User needs to improve an existing script's logic
        // Editing creates a new version - name is locked, but code and description can change

        datahubPage.scriptsTab.click()

        // Wait for script to appear in table
        datahubPage.scriptsTable.rows.should('have.length', 1)
        datahubPage.scriptsTable.cell(0, 'name').should('contain.text', 'temperature-converter')
        datahubPage.scriptsTable.cell(0, 'version').should('contain.text', '1')

        // Wait for edit button to be available and click it
        datahubPage.scriptsTable.action(0, 'edit').should('exist').click()

        // Verify editor opens in modify mode
        datahubPage.scriptEditor.drawer.should('be.visible')
        datahubPage.scriptEditor.title.should('contain', 'Create New Script Version')

        // Verify name is readonly
        datahubPage.scriptEditor.nameField.should('have.attr', 'readonly')
        datahubPage.scriptEditor.nameField.should('have.value', 'temperature-converter')

        // Verify version shows MODIFIED
        datahubPage.scriptEditor.versionDisplay.should('contain.text', 'MODIFIED')

        // Update the script source code using Monaco API
        const updatedCode = 'function convert(temp) { return (temp - 32) * 5/9; }'
        monacoEditor.setValue('#root_sourceCode', updatedCode)

        // Update description
        datahubPage.scriptEditor.descriptionField.clear().type('Updated: Converts F to C')

        // Save (intercept handled in beforeEach)
        datahubPage.scriptEditor.saveButton.click()
        cy.wait('@saveScript')

        // Verify success toast mentions version
        datahubPage.toast.shouldContain('version')

        // Verify drawer closed
        datahubPage.scriptEditor.drawer.should('not.exist')
      })
    })
  })

  describe('Policy Designer Integration', () => {
    it('should select existing schema in policy designer using simplified panel', () => {
      // NARRATIVE: User has existing schema and wants to use it in a policy
      // They open designer, drag schema node, and select the schema

      const schema: PolicySchema = {
        id: 'test-schema',
        version: 1,
        schemaDefinition: btoa(JSON.stringify(MOCK_SCHEMA_SOURCE)),
        type: 'JSON',
        createdAt: DateTime.now().toISO({ format: 'basic' }),
      }

      // Create schema in mock DB BEFORE opening designer
      mswDB.schema.create({
        id: schema.id,
        json: JSON.stringify(schema),
      })

      // Open policy designer
      datahubPage.addNewPolicy.click()
      datahubDesignerPage.canvas.should('be.visible')

      // CRITICAL: Create DATA_POLICY node FIRST - schema button is disabled without it
      datahubDesignerPage.toolbox.trigger.click()
      datahubDesignerPage.toolbox.dataPolicy.drag('[role="application"][data-testid="rf__wrapper"]')
      datahubDesignerPage.controls.fit.click()

      // Now drag schema node onto canvas
      datahubDesignerPage.toolbox.trigger.click()
      datahubDesignerPage.toolbox.schema.drag('[role="application"][data-testid="rf__wrapper"]')
      datahubDesignerPage.controls.fit.click()

      // Double-click schema node to open configuration panel
      datahubDesignerPage.designer.mode('SCHEMA').click().click()

      // // Wait for node editor panel to open
      // cy.getByTestId('node-editor-content').should('be.visible')
      //
      // Select schema from simplified panel
      datahubDesignerPage.schemaPanel.nameSelect.should('be.visible')
      datahubDesignerPage.schemaPanel.selectSchema('test-schema')

      // Verify version is displayed
      datahubDesignerPage.schemaPanel.versionDisplay.should('contain.text', '1')

      // SCREENSHOT 3: Simplified Panel - Schema Selected
      cy.percySnapshot('Policy Designer - Simplified Schema Panel')
      cy.screenshot('03-simplified-panel-schema-selected')

      // Submit form to apply changes
      datahubDesignerPage.schemaPanel.submitButton.click()

      // Verify node updates with selected schema
      datahubDesignerPage.designer.mode('SCHEMA').should('contain.text', 'test-schema')
    })

    it('should select existing script in policy designer using simplified panel', () => {
      // NARRATIVE: User has existing script and wants to use it in a policy
      // They open designer, drag function node, and select the script

      const script: Script = {
        id: 'test-script',
        version: 1,
        functionType: Script.functionType.TRANSFORMATION,
        source: btoa('function test() { return true; }'),
        createdAt: DateTime.now().toISO({ format: 'basic' }),
      }

      // Create script in mock DB BEFORE opening designer
      mswDB.script.create({
        id: script.id,
        json: JSON.stringify(script),
      })

      // Open policy designer
      datahubPage.addNewPolicy.click()
      datahubDesignerPage.canvas.should('be.visible')

      // Create DATA_POLICY node first
      datahubDesignerPage.toolbox.trigger.click()
      datahubDesignerPage.toolbox.dataPolicy.drag('[role="application"][data-testid="rf__wrapper"]')
      datahubDesignerPage.controls.fit.click()

      // Drag function node onto canvas
      datahubDesignerPage.toolbox.trigger.click()
      datahubDesignerPage.toolbox.function.drag('[role="application"][data-testid="rf__wrapper"]')
      datahubDesignerPage.controls.fit.click()

      // Double-click function node to open configuration panel
      datahubDesignerPage.designer.mode('FUNCTION').click().click()

      // Wait for node editor panel to open
      cy.getByTestId('node-editor-content').should('be.visible')

      // Select script from simplified panel
      datahubDesignerPage.scriptPanel.nameSelect.should('be.visible')
      datahubDesignerPage.scriptPanel.selectScript('test-script')

      // Verify version is displayed
      datahubDesignerPage.scriptPanel.versionDisplay.should('contain.text', '1')

      // Submit form to apply changes
      datahubDesignerPage.scriptPanel.submitButton.click()

      // Verify node updates with selected script
      datahubDesignerPage.designer.mode('FUNCTION').should('contain.text', 'test-script')
    })
  })

  describe('Complete Workflow', () => {
    it('should complete workflow: create schema → use in policy → validate', () => {
      // NARRATIVE: Complete user workflow from schema creation to policy usage
      // User creates a schema, then uses it in a new policy, and validates the policy

      const schemaName = 'workflow-test-schema'

      // Step 1: Create schema
      datahubPage.schemasTab.click()
      datahubPage.schemasTable.createButton.click()

      datahubPage.schemaEditor.drawer.should('be.visible')

      // Intercept schema creation and add to mock DB (BEFORE filling form)
      cy.intercept('POST', '/api/v1/data-hub/schemas', (req) => {
        const schema = req.body
        const response = {
          ...schema,
          version: 1,
          createdAt: DateTime.now().toISO({ format: 'basic' }),
        }
        req.reply({ statusCode: 201, body: response })
        mswDB.schema.create({
          id: response.id,
          json: JSON.stringify(response),
        })
      }).as('createSchema')

      datahubPage.schemaEditor.nameField.type(schemaName)
      datahubPage.schemaEditor.selectType('JSON')
      monacoEditor.setValue('#root_schemaSource', JSON.stringify(MOCK_SCHEMA_SOURCE, null, 2))

      datahubPage.schemaEditor.saveButton.click()
      cy.wait('@createSchema')

      // Wait for success toast
      datahubPage.toast.shouldContain('Schema')
      datahubPage.schemaEditor.drawer.should('not.exist')

      // Step 2: Create policy with schema node
      datahubPage.addNewPolicy.click()
      datahubDesignerPage.canvas.should('be.visible')

      // Add DATA_POLICY node
      datahubDesignerPage.toolbox.trigger.click()
      datahubDesignerPage.toolbox.dataPolicy.drag('[role="application"][data-testid="rf__wrapper"]')
      datahubDesignerPage.controls.fit.click()

      // Add SCHEMA node
      datahubDesignerPage.toolbox.trigger.click()
      datahubDesignerPage.toolbox.schema.drag('[role="application"][data-testid="rf__wrapper"]')
      datahubDesignerPage.controls.fit.click()

      // Step 3: Configure schema node to use created schema
      datahubDesignerPage.designer.mode('SCHEMA').click().click()
      cy.getByTestId('node-editor-content').should('be.visible')

      datahubDesignerPage.schemaPanel.nameSelect.should('be.visible')
      datahubDesignerPage.schemaPanel.selectSchema(schemaName)
      datahubDesignerPage.schemaPanel.versionDisplay.should('contain.text', '1')
      datahubDesignerPage.schemaPanel.submitButton.click()

      // Verify node updated
      datahubDesignerPage.designer.mode('SCHEMA').should('contain.text', schemaName)

      // Step 4: Validate policy (connection not required for basic validation)
      datahubDesignerPage.designer.selectNode('DATA_POLICY')
      datahubDesignerPage.toolbar.checkPolicy.should('be.visible').click()

      // Wait for validation to complete
      datahubDesignerPage.toolbar.checkPolicy.should('not.be.disabled')
    })
  })
})
