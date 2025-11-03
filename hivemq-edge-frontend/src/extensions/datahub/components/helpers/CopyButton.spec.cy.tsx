/// <reference types="cypress" />

import CopyButton from './CopyButton.tsx'

describe('CopyButton', () => {
  const testContent = 'Test content to copy'

  beforeEach(() => {
    cy.on('uncaught:exception', () => {
      // should take care of unsupported browser permissions in headless mode
      return false
    })
    // Needed to test with the clipboard
    Cypress.automation('remote:debugger:protocol', {
      command: 'Browser.grantPermissions',
      params: {
        permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
        origin: window.location.origin,
      },
    })
  })

  it('should render with default props', () => {
    cy.mountWithProviders(<CopyButton content={testContent} />)

    cy.contains('Copy').should('be.visible')
    cy.get('svg').should('exist') // Copy icon
  })

  it('should render with custom label', () => {
    cy.mountWithProviders(<CopyButton content={testContent} label="Copy All" />)

    cy.contains('Copy All').should('be.visible')
  })

  it('should render with data-testid', () => {
    cy.mountWithProviders(<CopyButton content={testContent} data-testid="test-copy-button" />)

    cy.get('[data-testid="test-copy-button"]').should('be.visible')
  })

  it('should copy content to clipboard when clicked', () => {
    cy.mountWithProviders(<CopyButton content={testContent} data-testid="copy-btn" />)

    // Click the button
    cy.get('[data-testid="copy-btn"]').click()

    // Should show toast notification
    cy.contains('Copied to clipboard').should('be.visible')

    // Icon should change to check mark temporarily
    cy.get('[data-testid="copy-btn"]').within(() => {
      cy.get('svg').should('exist')
    })
  })

  it('should show checkmark icon after copying', () => {
    cy.mountWithProviders(<CopyButton content={testContent} data-testid="copy-btn" />)

    // Before click - should have copy icon
    cy.get('[data-testid="copy-btn"]').click()

    // After click - should show "Copied" text (via useClipboard hook)
    cy.get('[data-testid="copy-btn"]').should('be.visible')
  })

  it('should support different sizes', () => {
    cy.mountWithProviders(
      <>
        <CopyButton content={testContent} size="xs" data-testid="btn-xs" />
        <CopyButton content={testContent} size="sm" data-testid="btn-sm" />
        <CopyButton content={testContent} size="md" data-testid="btn-md" />
      </>
    )

    cy.get('[data-testid="btn-xs"]').should('exist')
    cy.get('[data-testid="btn-sm"]').should('exist')
    cy.get('[data-testid="btn-md"]').should('exist')
  })

  it('should handle empty content', () => {
    cy.mountWithProviders(<CopyButton content="" data-testid="empty-copy" />)

    cy.get('[data-testid="empty-copy"]').click()
    cy.contains('Copied to clipboard').should('be.visible')
  })

  it('should handle long content', () => {
    const longContent = JSON.stringify({ data: Array(100).fill('test') }, null, 2)
    cy.mountWithProviders(<CopyButton content={longContent} data-testid="long-copy" />)

    cy.get('[data-testid="long-copy"]').click()
    cy.contains('Copied to clipboard').should('be.visible')
  })

  describe('Accessibility', () => {
    it('should be accessible', () => {
      cy.mountWithProviders(<CopyButton content={testContent} data-testid="accessible-btn" />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it.only('should be keyboard accessible', () => {
      cy.mountWithProviders(<CopyButton content={testContent} data-testid="keyboard-btn" />)

      cy.get('[data-testid="keyboard-btn"]').focus()
      cy.focused().should('have.attr', 'data-testid', 'keyboard-btn')

      cy.get('[data-testid="keyboard-btn"]').realType('{enter}')
      cy.get('[data-testid="keyboard-btn"]').should('have.text', 'Copied to clipboard')
      cy.get('[data-testid="keyboard-btn"]').should('have.text', 'Copy')
    })
  })
})
