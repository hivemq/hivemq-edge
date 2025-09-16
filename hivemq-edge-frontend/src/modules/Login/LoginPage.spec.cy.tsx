/// <reference types="cypress" />

import LoginPage from '@/modules/Login/LoginPage.tsx'
// @ts-ignore an import is not working
import type { CyHttpMessages } from 'cypress/types/net-stubbing'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import type { GatewayConfiguration } from '@/api/__generated__'

const mockNoPayload: GatewayConfiguration = {
  ...mockGatewayConfiguration,
  firstUseInformation: {
    firstUse: false,
  },
  preLoginNotice: undefined,
}

describe('LoginPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should show spinner while loading the first-use payload', () => {
    const mockError = { title: 'This is an error message', code: 404 }
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({ statusCode: 404, status: 404, body: mockError })
    })

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
  })

  it('should report error when loading the first-use payload fails', () => {
    const mockError = { title: 'This is an error message', code: 404 }
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({ statusCode: 404, status: 404, body: mockError })
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)

    cy.wait('@getConfig').then((e) => {
      expect(e.response?.body).to.deep.equal(mockError)
    })
  })

  it('should not show the first-use message if not in the payload', () => {
    cy.intercept('/api/v1/frontend/configuration', (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply(mockNoPayload)
    }).as('getConfig')

    cy.mountWithProviders(<LoginPage />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getConfig')
    cy.getByTestId('loading-spinner').should('not.exist')
    cy.get("[role='alert']").should('not.exist')
  })

  it('should show the first-use message', () => {
    cy.intercept('/api/v1/frontend/configuration', { ...mockGatewayConfiguration, preLoginNotice: undefined }).as(
      'getConfig'
    )

    cy.mountWithProviders(<LoginPage />)
    cy.wait('@getConfig').then((e) => console.log('ddd', e))
    cy.get("[role='alert']").eq(0).should('be.visible')
    cy.get("[role='alert']")
      .eq(0)
      .find("div[data-status='info']")
      .should('contain.text', 'Welcome To HiveMQ Edge')
      .should('contain.text', mockGatewayConfiguration.firstUseInformation?.firstUseDescription)
  })
})
