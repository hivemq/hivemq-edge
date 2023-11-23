/// <reference types="cypress" />

import ClipboardCopyIconButton from './ClipboardCopyIconButton.tsx'

const MOCK_CONTENT = 'Text to copy to the clipboard'

describe('ClipboardCopyIconButton', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
    cy.on('uncaught:exception', () => {
      // should take care of unsupported browser permissions in headless mode
      return false
    })
    // Needed to test with the clipboard ss
    Cypress.automation('remote:debugger:protocol', {
      command: 'Browser.grantPermissions',
      params: {
        permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
        origin: window.location.origin,
      },
    })
  })

  it('should render properly', () => {
    cy.clock()
    cy.mountWithProviders(<ClipboardCopyIconButton content={MOCK_CONTENT} />)

    cy.get('button').should('have.attr', 'data-state', 'READY')
    cy.get('button').click()

    cy.get('button').should('have.attr', 'data-state', 'COPIED')
    cy.get('[role="tooltip"]').should('contain.text', 'Copied!')

    cy.window().then((win) => {
      win.navigator.clipboard.readText().then((text) => {
        expect(text).to.eq('Text to copy to the clipboard')
      })
    })

    cy.tick(1500)

    cy.get('button').should('have.attr', 'data-state', 'READY')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ClipboardCopyIconButton content={MOCK_CONTENT} />)
    cy.checkAccessibility()

    cy.get('button').focus()
    cy.get('button').click()
    cy.get('[role="tooltip"]').should('contain.text', 'Copied!')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] CTooltip seems to generate false positives
        'color-contrast': { enabled: false },
        region: { enabled: false },
      },
    })
  })
})
