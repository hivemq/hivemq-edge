/// <reference types="cypress" />

// @ts-ignore an import is not working
import { CyHttpMessages } from 'cypress/types/net-stubbing'
import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { loginPage } from '../../pages/Login/LoginPage.ts'

describe('Login Page', () => {
  beforeEach(() => {
    loginPage.visit()
    cy.intercept('/api/v1/auth/authenticate', mockAuthApi(mockValidCredentials))
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
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.checkAccessibility()
    cy.percySnapshot('The login page on loading')
  })

  it('should redirect to login', () => {
    cy.visit('/app')
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
