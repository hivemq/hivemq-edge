/// <reference types="cypress" />

import LoginPage from '@/modules/Login/LoginPage.tsx'
// @ts-ignore an import is not working
import type { CyHttpMessages } from 'cypress/types/net-stubbing'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import type { AuthMode, GatewayConfiguration } from '@/api/__generated__'

const mockNoPayload: GatewayConfiguration = {
  ...mockGatewayConfiguration,
  firstUseInformation: {
    firstUse: false,
  },
  preLoginNotice: undefined,
}

const interceptAuthMode = (modes: AuthMode['modes']) => cy.intercept('/api/v1/auth/mode', { modes }).as('getAuthMode')

describe('LoginPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    // Every login render needs the auth mode; default it to local for the tests not about that.
    interceptAuthMode(['USERNAME_PASSWORD'])
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
    cy.wait('@getConfig')
    cy.get("[role='alert']").eq(0).should('be.visible')
    cy.get("[role='alert']")
      .eq(0)
      .find("div[data-status='info']")
      .should('contain.text', 'Welcome To HiveMQ Edge')
      .should('contain.text', mockGatewayConfiguration.firstUseInformation?.firstUseDescription)
  })

  describe('authentication mode', () => {
    beforeEach(() => {
      cy.intercept('/api/v1/frontend/configuration', mockNoPayload).as('getConfig')
    })

    it('should wait for the auth mode before showing any login mechanism', () => {
      // Hold /auth/mode open: no local form and no SSO button should appear while it is pending.
      cy.intercept('/api/v1/auth/mode', (req) => {
        req.on('response', (res) => res.setDelay(1000))
        req.reply({ modes: ['OPEN_ID'] })
      }).as('getAuthMode')

      cy.mountWithProviders(<LoginPage />)
      cy.wait('@getConfig')
      cy.getByTestId('loading-spinner').should('be.visible')
      cy.getByTestId('loginPage-submit').should('not.exist')
      cy.getByTestId('loginPage-sso').should('not.exist')
    })

    it('should report an error when the auth mode fails, not fall back to local', () => {
      cy.intercept('/api/v1/auth/mode', { statusCode: 500, body: {} }).as('getAuthMode')

      cy.mountWithProviders(<LoginPage />)
      // The query retries once before erroring; allow for that before the error surfaces.
      cy.get("[role='alert']", { timeout: 10000 }).should('be.visible')
      // The local form must not be presented as a fallback for an unknown mode.
      cy.get('#username').should('not.exist')
      cy.getByTestId('loginPage-sso').should('not.exist')
    })

    it('should show only SSO in an OIDC-only deployment', () => {
      interceptAuthMode(['OPEN_ID'])

      cy.mountWithProviders(<LoginPage />)
      cy.wait('@getAuthMode')
      cy.getByTestId('loginPage-sso').should('be.visible')
      cy.get('#username').should('not.exist')
    })

    it('should show both mechanisms when both are enabled', () => {
      interceptAuthMode(['USERNAME_PASSWORD', 'OPEN_ID'])

      cy.mountWithProviders(<LoginPage />)
      cy.wait('@getAuthMode')
      cy.getByTestId('loginPage-submit').should('be.visible')
      cy.getByTestId('loginPage-sso').should('be.visible')
    })
  })
})
