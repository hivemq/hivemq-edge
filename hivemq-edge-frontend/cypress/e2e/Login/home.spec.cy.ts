import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { homePage, loginPage } from 'cypress/pages'

describe('Home Page', () => {
  beforeEach(() => {
    cy.intercept('https://api.github.com/repos/hivemq/hivemq-edge/releases', { statusCode: 203, log: false })
    cy.intercept('/api/v1/frontend/capabilities', { statusCode: 203, log: false })
    cy.intercept('/api/v1/frontend/notifications', { statusCode: 203, log: false })
    cy.intercept('/api/v1/auth/authenticate', mockAuthApi(mockValidCredentials))
    cy.intercept('/api/v1/frontend/configuration', {
      ...mockGatewayConfiguration,
      preLoginNotice: undefined,
    })
    loginPage.visit()
    loginPage.loginButton.click()
    homePage.navLink.click()
  })

  it('should be accessible', { tags: ['@percy'] }, () => {
    cy.injectAxe()

    cy.checkAccessibility()
    cy.percySnapshot('Page: Login')
  })
})
