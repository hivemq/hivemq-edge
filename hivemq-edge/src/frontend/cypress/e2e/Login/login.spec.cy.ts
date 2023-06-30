/// <reference types="cypress" />

import { loginPage } from '../../pages/Login/LoginPage.ts'
import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'

describe('Login Page', () => {
  beforeEach(() => {
    loginPage.visit()
    cy.intercept('/auth/authenticate', mockAuthApi(mockValidCredentials))
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.checkAccessibility()
    cy.percySnapshot('The login page on loading')
  })

  it('should redirect to login', () => {
    cy.visit('/')
    cy.url().should('contain', '/login')
  })

  it('should log in with correct credentials', () => {
    loginPage.userInput.type('admin')
    loginPage.passwordInput.type('password')
    loginPage.loginButton.click()
    cy.url().should('not.contain', '/login')
  })

  it('should display an error message with wrong credentials', () => {
    const userName = 'mock'
    const password = 'incorrect'

    cy.intercept('/auth/authenticate', mockAuthApi({ userName, password }))

    cy.url().should('contain', '/login')
    loginPage.userInput.type(userName)
    loginPage.passwordInput.type(password)
    loginPage.loginButton.click()
    cy.url().should('contain', '/login')
  })
})
