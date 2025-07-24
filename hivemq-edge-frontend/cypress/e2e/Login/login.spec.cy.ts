import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { loginPage } from 'cypress/pages'

describe('Login Page', () => {
  beforeEach(() => {
    cy.intercept('/api/v1/frontend/capabilities', { statusCode: 203, log: false })
    cy.intercept('/api/v1/frontend/notifications', { statusCode: 203, log: false })
    cy.intercept('/api/v1/auth/authenticate', mockAuthApi(mockValidCredentials))
    cy.intercept('/api/v1/frontend/configuration', {
      ...mockGatewayConfiguration,
      firstUseInformation: {
        prefillUsername: null,
        prefillPassword: null,
        firstUseTitle: null,
        firstUseDescription: null,
      },
    })
    loginPage.visit()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.checkAccessibility()
    cy.percySnapshot('The login page on loading')
  })

  it('should redirect to login', () => {
    cy.visit('/app/')
    cy.url().should('contain', '/login')
  })

  it('should log in with correct credentials', () => {
    loginPage.userInput.type('admin')
    loginPage.passwordInput.type('hivemq')
    loginPage.loginButton.click()
    cy.url().should('not.contain', '/login')
  })

  it('should display an error message with wrong credentials', () => {
    const userName = 'mock'
    const password = 'incorrect'

    cy.intercept('/api/v1/auth/authenticate', mockAuthApi({ userName, password }))

    cy.url().should('contain', '/login')
    loginPage.userInput.type(userName)
    loginPage.passwordInput.type(password)
    loginPage.loginButton.click()
    cy.url().should('contain', '/login')
  })
})
