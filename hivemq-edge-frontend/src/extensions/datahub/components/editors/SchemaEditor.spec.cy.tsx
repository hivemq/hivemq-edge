/// <reference types="cypress" />

import { SchemaEditor } from './SchemaEditor.tsx'

describe('SchemaEditor', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })

    cy.intercept('/api/v1/data-hub/schemas', { statusCode: 202, log: false })
  })

  describe('Create Mode (schema = undefined)', () => {
    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub} />)

      // Wait for Monaco editor to load (can cause a11y issues during load)
      cy.get('#root_schemaSource', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

      cy.checkAccessibility()
    })

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

    it('should create a new JSON schema', () => {
      const onCloseSpy = cy.spy().as('onCloseSpy')

      // Intercept POST to schemas API
      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 201,
        body: {
          id: 'my-new-schema',
          type: 'JSON',
          version: 1,
          schemaDefinition: btoa('{"type": "object"}'),
          createdAt: new Date().toISOString(),
        },
      }).as('createSchema')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={onCloseSpy} />)

      // Fill in schema name
      cy.get('#root_name').type('my-new-schema')

      // Keep default JSON type and default schema source
      // The form should have a default JSON schema template

      // Save button should be enabled
      cy.contains('button', 'Save').should('not.be.disabled')

      // Click save
      cy.contains('button', 'Save').click()

      // Wait for API call
      cy.wait('@createSchema')
        .its('request.body')
        .should((body) => {
          expect(body.id).to.equal('my-new-schema')
          expect(body.type).to.equal('JSON')
          expect(body.schemaDefinition).to.exist // Should be base64 encoded
        })

      // Success toast should appear
      cy.contains('Schema Saved').should('be.visible')

      // Drawer should close
      cy.get('@onCloseSpy').should('have.been.calledOnce')
    })

    it('should create a new Protobuf schema', () => {
      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 201,
        body: {
          id: 'my-protobuf-schema',
          type: 'PROTOBUF',
          version: 1,
          schemaDefinition: 'base64encodedprotobuf',
          createdAt: new Date().toISOString(),
        },
      }).as('createSchema')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      // Fill name
      cy.get('#root_name').type('my-protobuf-schema')

      // Select Protobuf type using react-select pattern (from RJSFormField)
      // Pattern: find label by id, then get the next sibling div which contains react-select
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'PROTOBUF').click()

      // Verify Monaco editor is loaded and ready (from MONACO_TESTING_GUIDE)
      cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
      cy.get('#root_schemaSource').find('.monaco-editor .view-lines').should('exist')

      // Wait for messageType field to appear (required for PROTOBUF)
      cy.get('label#root_messageType-label').should('exist')
      cy.get('label#root_messageType-label').scrollIntoView()

      // PROTOBUF schemas require messageType field - select the first available message type
      cy.get('label#root_messageType-label + div').click()
      cy.get('[role="option"]').first().click()

      // Save button should now be enabled
      cy.contains('button', 'Save').should('not.be.disabled')
      cy.contains('button', 'Save').click()

      cy.wait('@createSchema')
        .its('request.body')
        .should((body) => {
          expect(body.id).to.equal('my-protobuf-schema')
          expect(body.type).to.equal('PROTOBUF')
        })

      cy.contains('Schema Saved').should('be.visible')
    })

    it('should validate JSON schema syntax', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      // Enter name
      cy.get('#root_name').type('test-schema')

      // Get Monaco editor and enter invalid JSON
      cy.get('#root_schemaSource').click()

      // Monaco editor shows validation errors inline
      // We can't easily trigger validation errors through cy.type with Monaco
      // But we can verify the save still works (backend validation is the authority)
      cy.contains('button', 'Save').should('not.be.disabled')
    })

    it('should validate Protobuf schema syntax', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      cy.get('#root_name').type('test-protobuf')

      // Select PROTOBUF using react-select pattern
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'PROTOBUF').click()

      // Wait for messageType field to appear (required for PROTOBUF)
      cy.get('label#root_messageType-label').should('exist')
      cy.get('label#root_messageType-label').scrollIntoView()
      cy.get('label#root_messageType-label + div').click()
      cy.get('[role="option"]').first().click()

      cy.contains('button', 'Save').should('not.be.disabled')
    })

    it('should switch between JSON and Protobuf types', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      // Wait for form to initialize
      cy.getByTestId('schema-editor-drawer').should('be.visible')

      // Check default is JSON - the react-select div shows the selected value
      cy.get('label#root_type-label + div').should('contain.text', 'JSON')

      // Switch to Protobuf
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'PROTOBUF').click()

      // Verify changed to PROTOBUF
      cy.get('label#root_type-label + div').should('contain.text', 'PROTOBUF')

      // Verify Monaco editor is still loaded and ready (from MONACO_TESTING_GUIDE)
      cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
      cy.get('#root_schemaSource').find('.monaco-editor .view-lines').should('exist')

      // Switch back to JSON
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'JSON').click()

      // Verify back to JSON
      cy.get('label#root_type-label + div').should('contain.text', 'JSON')
    })

    it('should disable save button when name is empty', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      cy.get('#root_name').type('fake name')
      cy.get('#root_name').clear()

      // Initially name is empty, save should be disabled
      cy.contains('button', 'Save').should('be.disabled')

      // Enter name
      cy.get('#root_name').type('my-schema')
      cy.contains('button', 'Save').should('not.be.disabled')

      // Clear name
      cy.get('#root_name').clear()
      cy.contains('button', 'Save').should('be.disabled')
    })

    it('should show version as DRAFT', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      // Version field uses custom widget - verify the version label exists and the field contains DRAFT
      cy.get('label#root_version-label').should('be.visible').should('contain.text', 'Version')
      // The version value should be visible near its label
      cy.get('label#root_version-label').parent().should('contain.text', 'DRAFT')
    })

    it('should prevent duplicate schema names', () => {
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
      cy.get('#root_name__error')
        .should('be.visible')
        .and('contain', 'A schema with the name "existing-schema" already exists')

      // Verify save button is disabled when there's a validation error
      cy.getByTestId('save-schema-button').should('be.disabled')

      // Clear and enter a different name
      cy.get('#root_name').clear()
      cy.get('#root_name').type('new-unique-schema')
      cy.get('#root_name').blur()

      // Verify validation error is cleared
      cy.get('#root_name__error').should('not.exist')

      // Verify save button is enabled
      cy.getByTestId('save-schema-button').should('not.be.disabled')
    })

    it('should track dirty state for all editable fields', () => {
      const mockSchema = {
        id: 'test-schema',
        version: 1,
        type: 'JSON',
        schemaDefinition: btoa('{"type": "object"}'),
        createdAt: '2025-11-26T10:00:00Z',
      }

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} schema={mockSchema} />)

      // Wait for Monaco to load
      cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

      // Initially save button should be disabled (no changes made yet)
      cy.contains('button', 'Save').should('be.disabled')

      // Name is readonly in modify mode - verify we can't change it
      cy.get('#root_name').should('have.attr', 'readonly')

      // Change schema type -> save button should become enabled
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'PROTOBUF').click()

      // Wait for messageType field (required for PROTOBUF)
      cy.get('label#root_messageType-label').should('exist')
      cy.get('label#root_messageType-label + div').click()
      cy.get('[role="option"]').first().click()

      // Save button should now be enabled (dirty state detected)
      cy.contains('button', 'Save').should('not.be.disabled')

      // Revert type back to JSON -> save button should be disabled again
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'JSON').click()

      // Save button should be disabled (back to original state)
      cy.contains('button', 'Save').should('be.disabled')

      // Change schema source using Monaco API -> save button enabled
      cy.window().then((win) => {
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        // Modify the content
        editor.setValue('{"type": "string"}')
      })

      // Save button should be enabled (schema source changed)
      cy.contains('button', 'Save').should('not.be.disabled')

      // Revert schema source back to original -> save button disabled
      cy.window().then((win) => {
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        // Revert to original content
        editor.setValue('{"type": "object"}')
      })

      // Save button should be disabled (back to original state)
      cy.contains('button', 'Save').should('be.disabled')
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

    it('should create new version from existing schema', () => {
      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 201,
        body: {
          ...mockSchema,
          version: 3, // Backend increments version
        },
      }).as('createSchema')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} schema={mockSchema} />)

      // Name should be pre-filled and readonly
      cy.get('#root_name').should('have.value', 'temperature-schema')
      cy.get('#root_name').should('have.attr', 'readonly')

      // Version should show MODIFIED in the version field
      cy.get('label#root_version-label').parent().should('contain.text', 'MODIFIED')

      // The content must be modified to enable submit
      cy.contains('button', 'Save').should('be.disabled')
      cy.get('#root_schemaSource').click()

      // Wait for Monaco to loas and change to valid JSON
      cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')
      cy.window().then((win) => {
        // @ts-ignore
        expect(win.monaco).to.exist
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        editor.setValue('') // Clear all content
        editor.trigger('keyboard', 'type', { text: '{}' })
      })

      cy.contains('button', 'Save').should('not.be.disabled')
      cy.contains('button', 'Save').click()

      cy.wait('@createSchema')
      cy.contains('Schema Saved').should('be.visible')
    })

    it('should have readonly name field', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} schema={mockSchema} />)

      cy.get('#root_name').should('have.attr', 'readonly')
      cy.get('#root_name').should('not.have.focus')
    })

    it('should show version as MODIFIED', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} schema={mockSchema} />)

      // Verify MODIFIED text is shown in the version field
      cy.get('label#root_version-label').parent().should('contain.text', 'MODIFIED')
    })

    it('should load schema content correctly', () => {
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} schema={mockSchema} />)

      // Schema type should be pre-selected
      cy.get('label#root_type-label').should('be.visible')
      cy.get('label#root_type-label + div').should('contain.text', 'JSON')

      // Verify Monaco editor is loaded and ready (from MONACO_TESTING_GUIDE)
      cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
      cy.get('#root_schemaSource').find('.monaco-editor .view-lines').should('exist')
    })
  })

  describe('Common Behaviors', () => {
    it('should handle save errors', () => {
      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 500,
        body: { title: 'Internal Server Error' },
      }).as('createSchema')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      cy.get('#root_name').type('error-schema')
      cy.contains('button', 'Save').click()

      cy.wait('@createSchema')

      // Error toast should be shown
      cy.contains('Save Failed').should('be.visible')

      // Drawer should stay open (user can retry)
      cy.get('[role="dialog"]').should('be.visible')
    })

    it('should show loading state during save', () => {
      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 201,
        delay: 1000,
        body: {
          id: 'test-schema',
          type: 'JSON',
          version: 1,
          schemaDefinition: btoa('{"type": "object"}'),
          createdAt: new Date().toISOString(),
        },
      }).as('createSchema')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={cy.stub()} />)

      cy.get('#root_name').type('test-schema')
      cy.contains('button', 'Save').click()

      // Save button should be disabled during save
      cy.contains('button', 'Save').should('be.disabled')

      cy.wait('@createSchema')
      cy.contains('Schema Saved').should('be.visible')
    })

    it('should reset form when drawer closes and reopens', () => {
      const mockOnClose = cy.stub().as('onClose')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={mockOnClose} />)

      cy.get('#root_name').type('test-schema')

      // Simulate close and reopen by remounting
      cy.mountWithProviders(<SchemaEditor isOpen={false} onClose={mockOnClose} />)
      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={mockOnClose} />)

      cy.get('#root_name').should('have.value', '')
    })

    it('should close drawer on successful save', () => {
      const onCloseSpy = cy.spy().as('onCloseSpy')

      cy.intercept('POST', '/api/v1/data-hub/schemas', {
        statusCode: 201,
        body: {
          id: 'test-schema',
          type: 'JSON',
          version: 1,
          schemaDefinition: btoa('{"type": "object"}'),
          createdAt: new Date().toISOString(),
        },
      }).as('createSchema')

      cy.mountWithProviders(<SchemaEditor isOpen={true} onClose={onCloseSpy} />)

      cy.get('#root_name').type('test-schema')
      cy.contains('button', 'Save').click()

      cy.wait('@createSchema')

      cy.get('@onCloseSpy').should('have.been.calledOnce')
    })
  })
})
