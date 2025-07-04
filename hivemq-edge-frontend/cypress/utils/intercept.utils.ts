import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'

export const cy_interceptCoreE2E = () => {
  // requests sent but not necessary to logic
  cy.intercept('https://api.github.com/repos/hivemq/hivemq-edge/releases', { statusCode: 202, log: false })
  cy.intercept('../api/v1/frontend/notifications', { statusCode: 202, log: false })
  cy.intercept('../api/v1/management/protocol-adapters/status', { statusCode: 202, log: false })
  cy.intercept('../api/v1/management/bridges/status', { statusCode: 202, log: false })
  cy.intercept('../api/v1/frontend/capabilities', { statusCode: 202, log: false })

  // code business requests
  cy.intercept('../api/v1/auth/authenticate', mockAuthApi(mockValidCredentials))
  cy.intercept('../api/v1/frontend/configuration', mockGatewayConfiguration)

  // Add a dummy element so we can check uniqueness
  cy.intercept('../api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] }).as('getAdapters')
}
