import { MOCK_CAPABILITIES, mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import WelcomePage from '@/modules/Welcome/WelcomePage.tsx'

describe('WelcomePage', () => {
  beforeEach(() => {
    cy.viewport(960, 800)
    cy.intercept('/api/v1/frontend/configuration', mockGatewayConfiguration)
    cy.intercept('/api/v1/frontend/capabilities', { items: [] })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<WelcomePage />)
    cy.checkAccessibility()
  })

  it('should render properly', () => {
    cy.mountWithProviders(<WelcomePage />)
    cy.get('header h1').should('have.text', 'Welcome to HiveMQ Edge')
    cy.get('header h1 + p').should(
      'have.text',
      'Connect to and from any device for seamless data streaming to your enterprise infrastructure.'
    )

    cy.getByTestId('onboarding-container').within(() => {
      cy.get('h2').should('have.text', 'Get data flowing')
      cy.get('aside').should('have.length', 4)
    })
  })

  describe('Pulse Onboarding', () => {
    it('should render activation properly', () => {
      cy.mountWithProviders(<WelcomePage />)

      cy.getByTestId('onboarding-container').within(() => {
        cy.get('h2').should('have.text', 'Get data flowing')
        cy.get('aside[aria-labelledby="heading-task-3"]').within(() => {
          cy.get('h3').should('have.text', 'Connect to HiveMQ Pulse')

          cy.get('section').within(() => {
            cy.get('p').should(
              'have.text',
              'To access the features of HiveMQ Edge Pulse, you need to activate it first.'
            )
            cy.get('button').should('have.text', 'Activate Pulse')
          })
        })
      })
    })

    it('should render todos properly', () => {
      cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
      cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 404 })
      cy.mountWithProviders(<WelcomePage />)

      cy.getByTestId('onboarding-container').within(() => {
        cy.get('h2').should('have.text', 'Get data flowing')
        cy.get('aside[aria-labelledby="heading-task-3"]').within(() => {
          cy.get('h3').should('have.text', 'Connect to HiveMQ Pulse')
          cy.get('section')
            .eq(0)
            .within(() => {
              cy.get('p').should(
                'have.text',
                'To access the features of HiveMQ Edge Pulse, you need to activate it first.'
              )
              cy.get('button').should('have.text', 'Manage Activation')
            })

          cy.get('section')
            .eq(1)
            .within(() => {
              cy.get('p').should('have.text', 'Use HiveMQ Edge Pulse to manage and publish assets to your HiveMQ Edge')
              cy.get('a').should('have.text', 'Manage Pulse Assets')
            })

          cy.get('section')
            .eq(2)
            .within(() => {
              cy.get('p').should('have.text', 'Stay up-to-date with your asset mappings')
              cy.get('[role="alert"]').should('have.text', 'Not Found').should('have.attr', 'data-status', 'error')
            })
        })
      })
    })
  })
})
