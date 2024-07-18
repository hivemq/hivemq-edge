/// <reference types="cypress" />

import ProtocolAdapterPage from '@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'

describe('ProtocolAdapterPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('api/v1/management/protocol-adapters/status', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
  })

  it('should render adapters for returning users', () => {
    cy.intercept('api/v1/frontend/configuration', mockGatewayConfiguration)
    cy.mountWithProviders(<ProtocolAdapterPage />)

    cy.get("[role='tab']").eq(1).should('have.attr', 'aria-selected', 'true')
  })

  it('should render protocols for new users', () => {
    const newUser = {
      ...mockGatewayConfiguration,
      firstUseInformation: { ...mockGatewayConfiguration, firstUse: true },
    }
    cy.intercept('api/v1/frontend/configuration', newUser)
    cy.mountWithProviders(<ProtocolAdapterPage />)

    cy.get("[role='tab']").eq(1).should('have.attr', 'aria-selected', 'true')
  })

  it('should be accessible', () => {
    const newUser = {
      ...mockGatewayConfiguration,
      firstUseInformation: { ...mockGatewayConfiguration, firstUse: true },
    }
    cy.intercept('api/v1/frontend/configuration', newUser).as('getConfig')
    cy.injectAxe()
    cy.mountWithProviders(<ProtocolAdapterPage />)

    cy.wait('@getConfig')
    cy.wait('@getAdapters')
    cy.wait('@getProtocols')
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
    cy.get("[role='tab']").eq(0).click()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
  })
})
