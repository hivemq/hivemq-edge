import { getPolicyPublishWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'
import { ToolbarPublish } from '@datahub/components/toolbar/ToolbarPublish.tsx'

describe('ToolbarPublish', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should not allow publishing without a report', () => {
    cy.mountWithProviders(<ToolbarPublish />, { wrapper: getPolicyPublishWrapper([]) })
    cy.get('button').should('have.text', 'Publish').should('be.disabled')
  })

  it('should handle errors', () => {
    cy.intercept('POST', '/api/v1/data-hub/data-validation/policies', { statusCode: 404 }).as('getPolicies')
    cy.mountWithProviders(<ToolbarPublish />, { wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]) })
    cy.get('button').should('have.text', 'Publish').should('not.be.disabled')

    cy.get('button').click()
    cy.wait('@getPolicies')
    cy.get('[role="status"] div').should('have.attr', 'data-status', 'error')
    cy.get('[role="status"] div#toast-1-title').should('have.text', 'Error publishing Data Policy')
    cy.get('[role="status"] div#toast-1-description').should('have.text', 'Not Found')
  })

  it('should handle success', () => {
    cy.intercept('POST', '/api/v1/data-hub/data-validation/policies', { statusCode: 200 }).as('getPolicies')

    cy.mountWithProviders(<ToolbarPublish />, { wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]) })
    cy.get('button').should('have.text', 'Publish').should('not.be.disabled')
    cy.get('button').click()
    cy.wait('@getPolicies')
    cy.get('[role="status"] div').should('have.attr', 'data-status', 'success')
    cy.get('[role="status"] div#toast-2-title').should('have.text', 'Data Policy published')
    cy.get('[role="status"] div#toast-2-description').should('have.text', "We've created a new Data Policy for you.")
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolbarPublish />, { wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]) })

    cy.checkAccessibility()
  })
})
