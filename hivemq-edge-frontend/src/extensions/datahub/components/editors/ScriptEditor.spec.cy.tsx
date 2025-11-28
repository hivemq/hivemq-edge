/// <reference types="cypress" />

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
    // ✅ ACTIVE - Accessibility testing
    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<ScriptEditor isOpen={true} onClose={cy.stub} />)

      // Wait for Monaco editor to load (can cause a11y issues during load)
      cy.get('#root_sourceCode', { timeout: 10000 }).find('.monaco-editor').should('be.visible')

      cy.checkAccessibility()
    })

    // ✅ ACTIVE - Keyboard navigation
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

    // ⏭️ SKIPPED - Will activate during Phase 4
    it.skip('should create a new script', () => {
      // Test: Fill form with script name and source
      // Test: Save button enabled when name filled
      // Test: POST to API with correct payload (base64 encoded)
      // Test: Success toast shown
      // Test: Drawer closes on success
    })

    it.skip('should validate JavaScript syntax', () => {
      // Test: Enter invalid JavaScript
      // Test: Monaco editor shows syntax errors
      // Test: Can still save (backend may validate further)
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

    it.skip('should allow optional description', () => {
      // Test: Description field is optional
      // Test: Can save without description
      // Test: Description is preserved when provided
    })

    it.skip('should prevent duplicate script names', () => {
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
      cy.get('#root_name-helper')
        .should('be.visible')
        .and('contain', 'A script with the name "existing-script" already exists')

      // Verify save button is disabled when there's a validation error
      cy.getByTestId('save-script-button').should('be.disabled')

      // Clear and enter a different name
      cy.get('#root_name').clear().type('new-unique-script')
      cy.get('#root_name').blur()

      // Verify validation error is cleared
      cy.get('#root_name-helper').should('not.exist')

      // Verify save button is enabled
      cy.getByTestId('save-script-button').should('not.be.disabled')
    })

    it.skip('should disable save button when form is not modified', () => {
      // Test: Intercept GET scripts with mockScript
      // Test: Mount ScriptEditor in modify mode with existing script
      // Test: Verify save button is initially disabled (no changes yet)
      // Test: Modify script source code
      // Test: Verify save button becomes enabled
      // Test: Revert changes back to original
      // Test: Verify save button becomes disabled again
    })

    it.skip('should track dirty state for all editable fields', () => {
      // Test: Mount ScriptEditor in modify mode
      // Test: Initially save button disabled (no changes)
      // Test: Change script name (should not be possible in modify mode - readonly)
      // Test: Change description -> save button enabled
      // Test: Revert description -> save button disabled
      // Test: Change source code -> save button enabled
      // Test: Revert source -> save button disabled
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

    // ✅ ACTIVE - Accessibility testing for modify mode
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

    // ⏭️ SKIPPED - Will activate during Phase 4
    it.skip('should create new version from existing script', () => {
      // Test: Pass script prop (e.g., version 2 of 5 existing versions)
      // Test: Form pre-populated with that script's data
      // Test: Name field is readonly
      // Test: Version shows "MODIFIED - a new version will be created"
      // Test: Save creates new version (backend auto-increments to highest + 1)
      // Test: Success toast shown with "New version created" message
    })

    it.skip('should have readonly name field', () => {
      // Test: Mount with script prop
      // Test: Name field is readonly
      // Test: Name field does not have autofocus
    })

    it.skip('should show version as MODIFIED', () => {
      // Test: Version field shows "MODIFIED - a new version will be created"
      // Test: Version field is readonly
    })

    it.skip('should load script content correctly', () => {
      // Test: Script source is decoded from base64
      // Test: Description is pre-filled if exists
      // Test: Can edit the script source
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
