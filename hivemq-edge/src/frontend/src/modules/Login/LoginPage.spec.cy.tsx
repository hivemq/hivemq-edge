/// <reference types="cypress" />

import LoginPage from '@/modules/Login/LoginPage.tsx'
// @ts-ignore an import is not working
import { CyHttpMessages } from 'cypress/types/net-stubbing'
import { mockGatewayConfiguration } from '@/api/hooks/useGatewayPortal/__handlers__'

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

    cy.wait('@getConfig')
    cy.wait('@getConfig')
    cy.wait('@getConfig')
    cy.wait('@getConfig').then((e) => {
      expect(e.response?.body).to.deep.equal(mockError)
    })
  })

  it('should show the first-use messages', () => {
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply(mockGatewayConfiguration)
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get("[role='alert'")
      .eq(0)
      .should('be.visible')
      .find("div[data-status='info']")
      .should('contain.text', 'Welcome To HiveMQ Edge')
      .should('contain.text', mockGatewayConfiguration.firstUseInformation?.firstUseDescription)
  })

  it('should show not show the messages', () => {
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({
        ...mockGatewayConfiguration,
        firstUseInformation: {
          prefillUsername: null,
          prefillPassword: null,
          firstUseTitle: null,
          firstUseDescription: null,
        },
      })
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.get("[role='alert']").should('not.exist')
  })
})
