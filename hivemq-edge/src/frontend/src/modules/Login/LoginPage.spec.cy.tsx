/// <reference types="cypress" />

import LoginPage from '@/modules/Login/LoginPage.tsx'
// @ts-ignore an import is not working
import { CyHttpMessages } from 'cypress/types/net-stubbing'
import { mockGatewayConfiguration } from '@/api/hooks/useGatewayPortal/__handlers__'

describe('LoginPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should show spinner while loading the first-time bundle', () => {
    const mockError = { title: 'This is an error message', code: 404 }
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({ statusCode: 404, status: 404, body: mockError })
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
  })

  it('should report error when loading the first-time bundle', () => {
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

  it('should show spinner while loading the first-time bundle', () => {
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply(mockGatewayConfiguration)
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
  })
})
