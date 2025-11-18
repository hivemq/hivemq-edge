import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import PolicyToolbar from '@datahub/components/toolbar/PolicyToolbar.tsx'
import { getPolicyPublishWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'

describe('PolicyToolbar', () => {
  beforeEach(() => {
    cy.viewport(800, 250)

    cy.intercept('/api/v1/data-hub/function-specs', {
      items: MOCK_DATAHUB_FUNCTIONS.items.map((specs) => {
        specs.metadata.inLicenseAllowed = true
        return specs
      }),
    })
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
