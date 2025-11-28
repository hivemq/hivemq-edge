/// <reference types="cypress" />

import { SchemaEditor } from './SchemaEditor.tsx'

describe('SchemaEditor', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  describe('Create Mode (schema = undefined)', () => {
    // ✅ ACTIVE - Accessibility testing
    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub} />)

      // Wait for Monaco editor to load (can cause a11y issues during load)
      cy.get('#root_schemaSource', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

      cy.checkAccessibility()
    })

    // ✅ ACTIVE - Keyboard navigation
    it('should support keyboard navigation', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub} />)

      // Verify name field has autofocus in create mode
      cy.get('#root_name').should('have.focus')

      // Test Tab navigation - essential for keyboard-only users
      cy.focused().realPress('Tab')
      cy.focused().should('have.attr', 'id', 'root_type')

      // Verify we can continue tabbing through form elements
      cy.focused().realPress('Tab')
      cy.focused().should('exist') // Should focus next element (version or schemaSource)
    })

    // ⏭️ SKIPPED - Will activate during Phase 4
    it.skip('should create a new JSON schema', () => {
      // Test: Fill form with JSON schema
      // Test: Save button enabled when name filled
      // Test: POST to API with correct payload (base64 encoded)
      // Test: Success toast shown
      // Test: Drawer closes on success
    })

    it.skip('should create a new Protobuf schema', () => {
      // Test: Select Protobuf type
      // Test: Schema source template changes
      // Test: Protobuf validation works
      // Test: Save creates Protobuf schema
    })

    it.skip('should validate JSON schema syntax', () => {
      // Test: Enter invalid JSON
      // Test: Validation error shown in form
      // Test: Save button works but backend may reject
    })

    it.skip('should validate Protobuf schema syntax', () => {
      // Test: Select Protobuf type
      // Test: Enter invalid Protobuf
      // Test: Validation error shown
    })

    it.skip('should switch between JSON and Protobuf types', () => {
      // Test: Start with JSON
      // Test: Switch to Protobuf
      // Test: Schema source template updates
      // Test: Switch back to JSON
      // Test: Template updates again
    })

    it.skip('should disable save button when name is empty', () => {
      // Test: Save button disabled initially (no name)
      // Test: Enable when name entered
      // Test: Disable when name cleared
    })

    it.skip('should show version as DRAFT', () => {
      // Test: Version field shows "DRAFT"
      // Test: Version field is readonly
    })

    it.skip('should prevent duplicate schema names', () => {
      const existingSchemas = [
        {
          id: 'existing-schema',
          type: 'JSON',
          schemaDefinition: btoa('{"type": "object"}'),
          version: 1,
          createdAt: '2025-11-26T10:00:00Z',
        },
      ]

      cy.intercept('GET', '/api/v1/data-hub/schemas', {
        statusCode: 200,
        body: { items: existingSchemas },
      }).as('getSchemas')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub} />)

      cy.wait('@getSchemas')

      // Enter a duplicate name
      cy.get('#root_name').type('existing-schema')

      // Wait for validation to trigger
      cy.get('#root_name').blur()

      // Verify validation error is shown
      cy.get('#root_name-helper')
        .should('be.visible')
        .and('contain', 'A schema with the name "existing-schema" already exists')

      // Verify save button is disabled when there's a validation error
      cy.getByTestId('save-schema-button').should('be.disabled')

      // Clear and enter a different name
      cy.get('#root_name').clear().type('new-unique-schema')
      cy.get('#root_name').blur()

      // Verify validation error is cleared
      cy.get('#root_name-helper').should('not.exist')

      // Verify save button is enabled
      cy.getByTestId('save-schema-button').should('not.be.disabled')
    })

    it.skip('should disable save button when form is not modified', () => {
      // Test: Intercept GET schemas with mockSchema
      // Test: Mount SchemaEditor in modify mode with existing schema
      // Test: Verify save button is initially disabled (no changes yet)
      // Test: Modify schema source code
      // Test: Verify save button becomes enabled
      // Test: Revert changes back to original
      // Test: Verify save button becomes disabled again
    })

    it.skip('should track dirty state for all editable fields', () => {
      // Test: Mount SchemaEditor in modify mode
      // Test: Initially save button disabled (no changes)
      // Test: Change schema name (should not be possible in modify mode - readonly)
      // Test: Change schema type -> save button enabled
      // Test: Revert type -> save button disabled
      // Test: Change schema source -> save button enabled
      // Test: Revert source -> save button disabled
    })
  })

  describe('Modify Mode (schema provided)', () => {
    const mockSchema = {
      id: 'temperature-schema',
      version: 2,
      type: 'JSON',
      schemaDefinition: btoa('{"type": "object", "properties": {"temperature": {"type": "number"}}}'),
      createdAt: '2025-11-26T10:00:00Z',
    }

    // ✅ ACTIVE - Accessibility testing for modify mode
    it('should be accessible', () => {
      // Intercept the create schema API call (used for creating new versions)
      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 500,
        body: {
          ...mockSchema,
          version: 3, // Backend would increment to version 3
        },
      }).as('createSchema')

      cy.injectAxe()
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub} schema={mockSchema} />)

      // Wait for Monaco editor to load (can cause a11y issues during load)
      cy.get('#root_schemaSource', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

      cy.checkAccessibility()
    })

    it.skip('should create new version from existing schema', () => {
      // Test: Pass schema prop (e.g., version 2 of 5 existing versions)
      // Test: Form pre-populated with that schema's data
      // Test: Name field is readonly
      // Test: Version shows "MODIFIED - a new version will be created"
      // Test: Save creates new version (backend auto-increments to highest + 1)
      // Test: Success toast shown with "New version created" message
    })

    it.skip('should have readonly name field', () => {
      // Test: Mount with schema prop
      // Test: Name field is readonly
      // Test: Name field does not have autofocus
    })

    it.skip('should show version as MODIFIED', () => {
      // Test: Version field shows "MODIFIED - a new version will be created"
      // Test: Version field is readonly
    })

    it.skip('should load schema content correctly', () => {
      // Test: Schema source is decoded from base64
      // Test: Schema type is pre-selected
      // Test: Can edit the schema source
    })
  })

  describe('Common Behaviors', () => {
    // ⏭️ SKIPPED - Will activate during Phase 4
    it.skip('should handle save errors', () => {
      // Test: API returns error
      // Test: Error toast shown
      // Test: Drawer stays open
      // Test: User can retry
    })

    it.skip('should show loading state during save', () => {
      // Test: isLoading on save button
      // Test: Cancel button disabled during save
    })

    it.skip('should reset form when drawer closes and reopens', () => {
      // Test: Fill form
      // Test: Close drawer
      // Test: Reopen drawer
      // Test: Form reset to initial state
    })

    it.skip('should close drawer on successful save', () => {
      // Test: Submit form
      // Test: Wait for API success
      // Test: onClose callback triggered
    })
  })
})
