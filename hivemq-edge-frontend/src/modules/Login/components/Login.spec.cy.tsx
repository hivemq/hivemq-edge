/// <reference types="cypress" />

import Login from '@/modules/Login/components/Login.tsx'
import { API_ROUTES } from '@cypr/support/__generated__/apiRoutes'

// A JWT the frontend's parseJWT/verifyJWT will accept: three segments, with a future `exp` claim.
const makeValidJwt = () => {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
  const payload = btoa(JSON.stringify({ sub: 'alice', roles: ['admin'], exp: Math.floor(Date.now() / 1000) + 3600 }))
  return `${header}.${payload}.signature`
}

describe('Login', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.interceptApi(API_ROUTES.authentication.authenticate, { token: 'fake_token' }).as('getConfig')

    cy.mountWithProviders(<Login />)
    // cy.get('.cxccc').should('contain.text', 'xxx')
    cy.get('#username').type('123')
    cy.get('#password').type('abc')

    cy.getByTestId('loginPage-submit').click()

    cy.wait('@getConfig').then(() => {
      cy.get("[role='alert']")
        .should('contain.text', 'Authentication Token')
        .should('contain.text', 'Your credentials have expired. Contact us')
    })
  })

  describe('SSO (OIDC)', () => {
    it('should NOT show the SSO button when SSO is not enabled', () => {
      cy.mountWithProviders(<Login />)

      cy.getByTestId('loginPage-submit').should('be.visible')
      cy.getByTestId('loginPage-sso').should('not.exist')
    })

    it('should show the SSO button when SSO is enabled', () => {
      cy.mountWithProviders(<Login ssoEnabled />)

      cy.getByTestId('loginPage-sso').should('be.visible').should('contain.text', 'Login with SSO')
    })

    it('should hide the username/password form when local login is disabled', () => {
      cy.mountWithProviders(<Login ssoEnabled localEnabled={false} />)

      cy.getByTestId('loginPage-submit').should('not.exist')
      cy.get('#username').should('not.exist')
      cy.getByTestId('loginPage-sso').should('be.visible')
    })

    it('should show only the SSO button when local login is disabled and SSO is the only mode', () => {
      cy.mountWithProviders(<Login ssoEnabled localEnabled={false} />)

      cy.getByTestId('loginPage-sso').should('be.visible')
      cy.get('form').should('not.exist')
    })

    it('should open the OIDC login popup when the SSO button is clicked', () => {
      const stubbedPopup = { closed: false, close: cy.stub() }
      cy.mountWithProviders(<Login ssoEnabled />, {
        wrapper: ({ children }) => {
          cy.stub(window, 'open').as('windowOpen').returns(stubbedPopup)
          return <>{children}</>
        },
      })

      cy.getByTestId('loginPage-sso').click()
      cy.get('@windowOpen')
        .should('have.been.calledOnce')
        .its('firstCall.args.0')
        .should('contain', '/api/v1/auth/oidc/login')
    })

    it('should sign in when the popup posts back a valid token', () => {
      const stubbedPopup = { closed: false, close: cy.stub() }
      cy.mountWithProviders(<Login ssoEnabled />, {
        wrapper: ({ children }) => {
          cy.stub(window, 'open').as('windowOpen').returns(stubbedPopup)
          return <>{children}</>
        },
      })

      cy.getByTestId('loginPage-sso').click()
      cy.get('@windowOpen').should('have.been.calledOnce')

      // Simulate the callback page posting the Edge JWT back to the opener.
      cy.window().then((win) => {
        win.postMessage({ token: makeValidJwt() }, win.location.origin)
      })

      // A valid token → no error alert (login succeeds and navigates away).
      cy.get("[role='alert']").should('not.exist')
    })

    it('should show an error when the popup is blocked', () => {
      cy.mountWithProviders(<Login ssoEnabled />, {
        wrapper: ({ children }) => {
          // window.open returning null models a blocked popup.
          cy.stub(window, 'open').as('windowOpen').returns(null)
          return <>{children}</>
        },
      })

      cy.getByTestId('loginPage-sso').click()

      cy.get("[role='alert']").should('contain.text', 'The sign-in window was blocked')
    })
  })

  it.skip('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Login />)
    cy.checkAccessibility()
  })
})
