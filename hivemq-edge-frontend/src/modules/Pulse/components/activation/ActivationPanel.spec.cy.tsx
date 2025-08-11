import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import { MOCK_CAPABILITY_PERSISTENCE, MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { ActivationPanel } from '@/modules/Pulse/components/activation/ActivationPanel.tsx'

describe('ActivationPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should handle errors', () => {
    cy.intercept('/api/v1/frontend/capabilities', { statusCode: 404 }).as('capabilities')

    cy.mountWithProviders(<ActivationPanel />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@capabilities')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'Cannot check the Pulse capability. Please try again later')
  })

  const cy_formShouldRenderProperly = (isActivated: boolean, title: string, description: string) => {
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@capabilities')
    cy.getByTestId('pulse-activation-trigger').should('have.text', 'Activate Pulse')
    cy.getByTestId('pulse-activation-trigger').click()
    cy.get("[role='dialog']").should('be.visible')
    cy.get("[role='dialog']").within(() => {
      cy.get('header').should('have.text', 'Pulse Client Activation')

      cy.get('[role="alert"]')
        .should('have.attr', 'data-status', 'info')
        // .should('contain.text', 'Pulse is activated')
        // .should('contain.text', 'You can revoke the activation or activate a different instance with a new token.')
        .should('contain.text', title)
        .should('contain.text', description)
    })

    cy.get('form#pulse-activation-form').within(() => {
      cy.getByTestId('root').within(() => {
        cy.get('textarea').should('have.attr', 'placeholder', 'Paste or drop the activation token here')
        cy.get('label[for="root"]').should('have.text', 'Activation Token')
        cy.get('textarea').should('have.text', '')
      })
    })

    cy.getByTestId('pulse-activation-revoke').should(isActivated ? 'not.be.disabled' : 'be.disabled')
    cy.getByTestId('pulse-activation-submit').should('be.disabled')

    cy.getByTestId('root').should('not.have.attr', 'data-invalid')
    cy.getByTestId('root').within(() => {
      cy.get('textarea').type('1234')
    })
    cy.getByTestId('root').should('have.attr', 'data-invalid')
    cy.get('#root__error').should(
      'have.text',
      'The token is not a completely valid JSON object conforming to JWT format'
    )
  }

  it('should handle activation', () => {
    cy.intercept('/api/v1/frontend/capabilities', {
      items: [MOCK_CAPABILITY_PERSISTENCE],
    }).as('capabilities')

    cy.mountWithProviders(<ActivationPanel />)
    cy_formShouldRenderProperly(
      false,
      'Pulse is not activated',
      'To activate Pulse, you need to submit the activation token.'
    )

    cy.getByTestId('root').within(() => {
      cy.get('textarea').clear()
    })
    cy.getByTestId('root').should('have.attr', 'data-invalid')
    cy.get('#root__error').should('have.text', 'must be string')

    cy.getByTestId('root').within(() => {
      cy.get('textarea').type(MOCK_JWT)
    })
    cy.getByTestId('root').should('not.have.attr', 'data-invalid')
    cy.getByTestId('pulse-activation-submit').should('not.be.disabled')

    cy.getByTestId('pulse-activation-submit').click()
  })

  it('should handle deactivation', () => {
    cy.intercept('/api/v1/frontend/capabilities', {
      items: [MOCK_CAPABILITY_PULSE_ASSETS],
    }).as('capabilities')

    cy.mountWithProviders(<ActivationPanel />)
    cy_formShouldRenderProperly(
      true,
      'Pulse is activated',
      'You can revoke the activation or activate a different instance with a new token.'
    )
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/frontend/capabilities', {
      items: [MOCK_CAPABILITY_PULSE_ASSETS],
    })
    cy.injectAxe()

    cy.mountWithProviders(<ActivationPanel />)
    cy.getByTestId('pulse-activation-trigger').click()

    cy.getByTestId('root').within(() => {
      cy.get('textarea').type('1234567890')
    })

    cy.checkAccessibility()
  })
})
