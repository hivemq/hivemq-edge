/// <reference types="cypress" />

import SwitchModeButton from '@/components/Chakra/SwitchModeButton.tsx'

describe('SwitchModeButton', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SwitchModeButton />)

    cy.get('body').should('have.class', 'chakra-ui-light')

    cy.getByTestId('chakra-ui-switch-mode').click()
    cy.get('body').should('have.class', 'chakra-ui-dark')

    cy.getByTestId('chakra-ui-switch-mode').click()
    cy.get('body').should('have.class', 'chakra-ui-light')
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] CTooltip seems to generate false positives
        region: { enabled: false },
      },
    })
    cy.percySnapshot('Component: SwitchModeButton')
  })
})
