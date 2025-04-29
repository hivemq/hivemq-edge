import PolicyToolbar from '@datahub/components/toolbar/PolicyToolbar.tsx'
import { getPolicyPublishWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'

describe('PolicyToolbar', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<PolicyToolbar />, {
      wrapper: getPolicyPublishWrapper(),
    })
    cy.getByTestId('toolbox-policy-check').should('not.be.disabled')
    cy.getByTestId('toolbox-policy-report').should('be.disabled')
    cy.getByTestId('toolbox-policy-clear').should('not.exist')
    cy.getByTestId('toolbox-policy-publish').should('be.disabled')
  })

  it('should render properly sss', () => {
    cy.mountWithProviders(<PolicyToolbar />, {
      wrapper: getPolicyPublishWrapper([{ node: MOCK_NODE_DATA_POLICY }]),
    })

    cy.getByTestId('toolbox-policy-check').should('not.be.disabled')
    cy.getByTestId('toolbox-policy-report').should('not.be.disabled')
    cy.getByTestId('toolbox-policy-clear').should('not.be.disabled')
    cy.getByTestId('toolbox-policy-publish').should('not.be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PolicyToolbar />, {
      wrapper: getPolicyPublishWrapper(),
    })
    cy.checkAccessibility()
  })
})
