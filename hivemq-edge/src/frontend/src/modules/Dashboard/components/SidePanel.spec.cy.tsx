/// <reference types="cypress" />

import SidePanel from '@/modules/Dashboard/components/SidePanel.tsx'

import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'

describe('SidePanel', () => {
  beforeEach(() => {
    cy.viewport(350, 800)
    cy.intercept('/api/v1/frontend/configuration', mockGatewayConfiguration).as('getConfig')
  })

  it('should contain the version', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SidePanel />)

    cy.getByTestId('edge-release').should('contain.text', '[ 2023.version ]')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SidePanel />)
    cy.checkAccessibility()
  })
})
