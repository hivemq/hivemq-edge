/// <reference types="cypress" />

import React from 'react'
import { Script } from '@/api/__generated__'
import { ScriptEditor } from './ScriptEditor.tsx'

describe('ScriptEditor', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  describe('Create Mode (script = undefined)', () => {
    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub} />)

      // Wait for Monaco editor to load (can cause a11y issues during load)
      cy.get('#root_sourceCode', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

      cy.checkAccessibility()
    })

    it('should support keyboard navigation', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub} />)

      // Verify name field has autofocus in create mode
      cy.get('#root_name').should('have.focus')

      // Test Tab navigation - essential for keyboard-only users
      cy.focused().realPress('Tab')
      cy.focused().should('have.attr', 'id', 'root_description')

      // Verify we can continue tabbing through form elements
      cy.focused().realPress('Tab')
      cy.focused().should('exist') // Should focus next element (version or sourceCode)
    })

    it('should create a new script', () => {
      const onCloseSpy = cy.spy().as('onCloseSpy')

      // Intercept POST to scripts API
      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 201,
        body: {
          id: 'my-new-script',
          functionType: Script.functionType.TRANSFORMATION,
          source: btoa('function transform(publish, context) { return publish; }'),
          version: 1,
          createdAt: new Date().toISOString(),
        },
      }).as('createScript')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={onCloseSpy} />)

      // Fill in script name
      cy.get('#root_name').type('my-new-script')

      // Keep default source code (template should be pre-filled)

      // Save button should be enabled (form is dirty after name entered)
      cy.contains('button', 'Save').should('not.be.disabled')

      // Click save
      cy.contains('button', 'Save').click()

      // Wait for API call
      cy.wait('@createScript')
        .its('request.body')
        .should((body) => {
          expect(body.id).to.equal('my-new-script')
          expect(body.functionType).to.equal(Script.functionType.TRANSFORMATION)
          expect(body.source).to.exist // Should be base64 encoded
        })

      // Success toast should appear
      cy.contains('Script Saved').should('be.visible')

      // Drawer should close
      cy.get('@onCloseSpy').should('have.been.calledOnce')
    })

    it('should validate JavaScript syntax', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} />)

      // Enter name
      cy.get('#root_name').type('test-script')

      // Get Monaco editor and enter invalid JavaScript
      cy.get('#root_sourceCode').click()

      // Verify Monaco editor is loaded
      cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

      // Test that form validates syntax on save attempt
      cy.window().then((win) => {
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        // Enter invalid JavaScript (syntax error)
        editor.setValue('function invalid( { // missing closing brace')
      })

      // Save button should be enabled (dirty state detected)
      cy.contains('button', 'Save').should('not.be.disabled')

      // Save button should now be disabled due to validation error
      cy.contains('button', 'Save').should('be.disabled')

      // Error message should appear in form (any syntax error message)
      cy.get('#root_sourceCode__error').should('be.visible').and('not.be.empty')
    })

    it('should disable save button when name is empty', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} />)

      // Initially name is empty, save should be disabled (not dirty)
      cy.contains('button', 'Save').should('be.disabled')

      // Enter name -> save button should be enabled (form is dirty)
      cy.get('#root_name').type('my-script')
      cy.contains('button', 'Save').should('not.be.disabled')

      // Clear name -> save button should be disabled (required field empty)
      cy.get('#root_name').clear()
      cy.contains('button', 'Save').should('be.disabled')
    })

    it('should show version as DRAFT', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} />)

      // Version field uses custom widget - verify the version label exists and the field contains DRAFT
      cy.get('label#root_version-label').should('be.visible').should('contain.text', 'Version')
      // The version value should be visible near its label
      cy.get('label#root_version-label').parent().should('contain.text', 'DRAFT')
    })

    it('should allow optional description', () => {
      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 201,
        body: {
          id: 'script-no-desc',
          functionType: Script.functionType.TRANSFORMATION,
          source: btoa('function transform(publish, context) { return publish; }'),
          version: 1,
          createdAt: new Date().toISOString(),
        },
      }).as('createScript')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} />)

      // Fill name but leave description empty
      cy.get('#root_name').type('script-no-desc')

      // Verify description field exists and is empty
      cy.get('#root_description').should('exist')
      cy.get('#root_description').should('have.value', '')

      // Save button should be enabled
      cy.contains('button', 'Save').should('not.be.disabled')
      cy.contains('button', 'Save').click()

      // Should save successfully without description
      cy.wait('@createScript')
        .its('request.body')
        .should((body) => {
          expect(body.id).to.equal('script-no-desc')
          // Description can be empty string or undefined when not provided
          expect(body.description === '' || body.description === undefined).to.be.true
        })

      cy.contains('Script Saved').should('be.visible')
    })

    it('should prevent duplicate script names', () => {
      const existingScripts = [
        {
          id: 'existing-script',
          functionType: Script.functionType.TRANSFORMATION as Script.functionType,
          source: btoa('function transform(publish, context) { return publish; }'),
          version: 1,
          createdAt: '2025-11-26T10:00:00Z',
        },
      ]

      cy.intercept('GET', '/api/v1/data-hub/scripts*', {
        statusCode: 200,
        body: { items: existingScripts },
      }).as('getScripts')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub} />)

      cy.wait('@getScripts')

      // Enter a duplicate name
      cy.get('#root_name').type('existing-script')

      // Wait for validation to trigger
      cy.get('#root_name').blur()

      // Verify validation error is shown
      cy.get('#root_name__error')
        .should('be.visible')
        .and('contain', 'A script with the name "existing-script" already exists')

      // Verify save button is disabled when there's a validation error
      cy.getByTestId('save-script-button').should('be.disabled')

      // Clear and enter a different name
      cy.get('#root_name').clear()
      cy.get('#root_name').type('new-unique-script')
      cy.get('#root_name').blur()

      // Verify validation error is cleared
      cy.get('#root_name__error').should('not.exist')

      // Verify save button is enabled
      cy.getByTestId('save-script-button').should('not.be.disabled')
    })

    it('should track dirty state for all editable fields', () => {
      const mockScript: Script = {
        id: 'test-script',
        version: 1,
        functionType: Script.functionType.TRANSFORMATION,
        source: btoa('function transform(publish, context) { return publish; }'),
        description: 'Original description',
        createdAt: '2025-11-26T10:00:00Z',
      }

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} script={mockScript} />)

      // Wait for Monaco to load
      cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

      // Initially save button should be disabled (no changes made yet)
      cy.contains('button', 'Save').should('be.disabled')

      // Name is readonly in modify mode - verify we can't change it
      cy.get('#root_name').should('have.attr', 'readonly')

      // Change description -> save button should become enabled
      cy.get('#root_description').clear()
      cy.get('#root_description').type('New description')

      // Save button should now be enabled (dirty state detected)
      cy.contains('button', 'Save').should('not.be.disabled')

      // Revert description back to original -> save button should be disabled again
      cy.get('#root_description').clear()
      cy.get('#root_description').type('Original description')

      // Save button should be disabled (back to original state)
      cy.contains('button', 'Save').should('be.disabled')

      // Change source code using Monaco API -> save button enabled
      cy.window().then((win) => {
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        // Modify the content
        editor.setValue('function transform(publish, context) { return {}; }')
      })

      // Save button should be enabled (source code changed)
      cy.contains('button', 'Save').should('not.be.disabled')

      // Revert source code back to original -> save button disabled
      cy.window().then((win) => {
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        // Revert to original content
        editor.setValue('function transform(publish, context) { return publish; }')
      })

      // Save button should be disabled (back to original state)
      cy.contains('button', 'Save').should('be.disabled')
    })
  })

  describe('Modify Mode (script provided)', () => {
    const mockScript: Script = {
      id: 'temperature-converter',
      version: 2,
      functionType: Script.functionType.TRANSFORMATION as Script.functionType,
      source: btoa('function transform(publish, context) { return publish; }'),
      description: 'Converts temperature data',
      createdAt: '2025-11-26T10:00:00Z',
    }

    it('should be accessible', () => {
      // Intercept the create script API call (used for creating new versions)
      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 200,
        body: {
          ...mockScript,
          version: 3, // Backend would increment to version 3
        },
      }).as('createScript')

      cy.injectAxe()
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub} script={mockScript} />)

      // Wait for Monaco editor to load (can cause a11y issues during load)
      cy.get('#root_sourceCode', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

      cy.checkAccessibility()
    })

    it('should create new version from existing script', () => {
      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 201,
        body: {
          ...mockScript,
          version: 3, // Backend increments version
        },
      }).as('createScript')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} script={mockScript} />)

      // Name should be pre-filled and readonly
      cy.get('#root_name').should('have.value', 'temperature-converter')
      cy.get('#root_name').should('have.attr', 'readonly')

      // Version should show MODIFIED in the version field
      cy.get('label#root_version-label').parent().should('contain.text', 'MODIFIED')

      // The content must be modified to enable submit
      cy.contains('button', 'Save').should('be.disabled')
      cy.get('#root_sourceCode').click()

      // Wait for Monaco to load and change the source
      cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')
      cy.window().then((win) => {
        // @ts-ignore
        expect(win.monaco).to.exist
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]

        editor.setValue('function transform(publish, context) { return {}; }')
      })

      cy.contains('button', 'Save').should('not.be.disabled')
      cy.contains('button', 'Save').click()

      cy.wait('@createScript')
      cy.contains('Script Saved').should('be.visible')
    })

    it('should have readonly name field', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} script={mockScript} />)

      cy.get('#root_name').should('have.attr', 'readonly')
      cy.get('#root_name').should('not.have.focus')
    })

    it('should show version as MODIFIED', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} script={mockScript} />)

      // Verify MODIFIED text is shown in the version field
      cy.get('label#root_version-label').parent().should('contain.text', 'MODIFIED')
    })

    it('should load script content correctly', () => {
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} script={mockScript} />)

      // Description should be pre-filled
      cy.get('#root_description').should('have.value', 'Converts temperature data')

      // Verify Monaco editor is loaded and ready
      cy.get('#root_sourceCode').find('.monaco-editor').should('be.visible')
      cy.get('#root_sourceCode').find('.monaco-editor .view-lines').should('exist')

      // Verify source code is loaded (decoded from base64)
      cy.window().then((win) => {
        // @ts-ignore
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]
        const content = editor.getValue()

        expect(content).to.include('function transform(publish, context)')
      })
    })
  })

  describe('Common Behaviors', () => {
    it('should handle save errors', () => {
      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 500,
        body: { title: 'Internal Server Error' },
      }).as('createScript')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} />)

      cy.get('#root_name').type('error-script')
      cy.contains('button', 'Save').click()

      cy.wait('@createScript')

      // Error toast should be shown
      cy.contains('Save Failed').should('be.visible')

      // Drawer should stay open (user can retry)
      cy.get('[role="dialog"]').should('be.visible')
    })

    it('should show loading state during save', () => {
      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 201,
        delay: 1000,
        body: {
          id: 'test-script',
          functionType: Script.functionType.TRANSFORMATION,
          source: btoa('function transform(publish, context) { return publish; }'),
          version: 1,
          createdAt: new Date().toISOString(),
        },
      }).as('createScript')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub()} />)

      cy.get('#root_name').type('test-script')
      cy.contains('button', 'Save').click()

      // Save button should be disabled during save
      cy.contains('button', 'Save').should('be.disabled')

      cy.wait('@createScript')
      cy.contains('Script Saved').should('be.visible')
    })

    it('should reset form when drawer closes and reopens', () => {
      const TestWrapper: React.FC = () => {
        const [isOpen, setIsOpen] = React.useState(true)

        return (
          <React.Fragment>
            <button onClick={() => setIsOpen(true)}>Open</button>
            <ScriptEditor isOpen={isOpen} onClose={() => setIsOpen(false)} />
          </React.Fragment>
        )
      }

      cy.mountWithProviders(<TestWrapper />)

      // Fill form
      cy.get('#root_name').type('test-script')
      cy.get('#root_description').type('Test description')

      // Close drawer
      cy.get('[aria-label="Close"]').click()

      // Verify drawer closed
      cy.get('[role="dialog"]').should('not.exist')

      // Reopen drawer
      cy.contains('button', 'Open').click()

      // Verify form reset to initial state
      cy.get('#root_name').should('have.value', '')
      cy.get('#root_description').should('have.value', '')
    })

    it('should close drawer on successful save', () => {
      const onCloseSpy = cy.spy().as('onCloseSpy')

      cy.intercept('POST', '/api/v1/data-hub/scripts', {
        statusCode: 201,
        body: {
          id: 'test-script',
          functionType: Script.functionType.TRANSFORMATION,
          source: btoa('function transform(publish, context) { return publish; }'),
          version: 1,
          createdAt: new Date().toISOString(),
        },
      }).as('createScript')

      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={onCloseSpy} />)

      cy.get('#root_name').type('test-script')
      cy.contains('button', 'Save').click()

      cy.wait('@createScript')
      cy.contains('Script Saved').should('be.visible')

      // onClose callback should be triggered
      cy.get('@onCloseSpy').should('have.been.calledOnce')
    })
  })
})
