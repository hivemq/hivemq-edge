/// <reference types="cypress" />

import LoginPage from '@/modules/Login/LoginPage.tsx'
// @ts-ignore an import is not working
import { CyHttpMessages } from 'cypress/types/net-stubbing'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { GatewayConfiguration } from '@/api/__generated__'

const mockNoPayload: GatewayConfiguration = {
  ...mockGatewayConfiguration,
  firstUseInformation: {
    firstUse: false,
  },
}

describe('LoginPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should show spinner while loading the first-use payload', () => {
    const mockError = { title: 'This is an error message', code: 404 }
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({ statusCode: 404, status: 404, body: mockError })
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
  })

  it('should report error when loading the first-use payload fails', () => {
    const mockError = { title: 'This is an error message', code: 404 }
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({ statusCode: 404, status: 404, body: mockError })
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)

    // cy.wait('@getConfig')
    // cy.wait('@getConfig')
    // cy.wait('@getConfig')
    cy.wait('@getConfig').then((e) => {
      expect(e.response?.body).to.deep.equal(mockError)
    })
  })

  // TODO[NVL] Weird conflict. Need resolution
  it.skip('should not show the message if it is not in the payload', () => {
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply(mockNoPayload)
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getConfig')
    cy.getByTestId('loading-spinner').should('not.exist')
    cy.get("[role='alert'").should('not.exist')
  })

  it('should show the first-use messages', () => {
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply(mockGatewayConfiguration)
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.wait('@getConfig').then((e) => console.log('ddd', e))
    cy.get("[role='alert'")
      .eq(0)
      .should('be.visible')
      .find("div[data-status='info']")
      .should('contain.text', 'Welcome To HiveMQ Edge')
      .should('contain.text', mockGatewayConfiguration.firstUseInformation?.firstUseDescription)
  })
})
