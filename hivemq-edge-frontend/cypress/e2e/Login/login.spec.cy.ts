import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { loginPage } from 'cypress/pages'

describe('Login Page', () => {
  const userName = 'mock'
  const password = 'incorrect'

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
      preLoginNotice: undefined,
    })
    loginPage.visit()
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
    cy.intercept('/api/v1/auth/authenticate', mockAuthApi({ userName, password }))

    cy.url().should('contain', '/login')
    loginPage.userInput.type(userName)
    loginPage.passwordInput.type(password)
    loginPage.loginButton.click()
    cy.url().should('contain', '/login')
  })

  it('should be accessible', { tags: ['@percy'] }, () => {
    cy.injectAxe()

    loginPage.userInput.type(userName)
    loginPage.passwordInput.type(password)
    loginPage.showPassword.click()

    cy.checkAccessibility()
    cy.percySnapshot('Page: Onboarding')
  })

  it('should capture login error state', { tags: ['@percy'] }, () => {
    cy.injectAxe()
    cy.intercept('/api/v1/auth/authenticate', mockAuthApi({ userName, password }))

    loginPage.userInput.type(userName)
    loginPage.passwordInput.type(password)
    loginPage.loginButton.click()

    // Wait for error message to appear
    loginPage.errorMessage.should('be.visible')

    cy.checkAccessibility()
    cy.percySnapshot('Login - Error State')
  })
})
