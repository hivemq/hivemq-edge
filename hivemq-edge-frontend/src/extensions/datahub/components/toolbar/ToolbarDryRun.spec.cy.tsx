import { getPolicyPublishWrapper } from '@datahub/__test-utils__/react-flow.mocks.tsx'
import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { ToolbarDryRun } from '@datahub/components/toolbar/ToolbarDryRun.tsx'
import { PolicyDryRunStatus } from '@datahub/types.ts'

describe('ToolbarDryRun', () => {
  beforeEach(() => {
    cy.viewport(800, 600)

    cy.intercept('/api/v1/data-hub/function-specs', {
      items: MOCK_DATAHUB_FUNCTIONS.items.map((specs) => {
        specs.metadata.inLicenseAllowed = true
        return specs
      }),
    })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<ToolbarDryRun />, { wrapper: getPolicyPublishWrapper() })

    cy.getByTestId('toolbox-policy-check')
      .should('have.text', 'Check')
      .should('have.attr', 'data-status', PolicyDryRunStatus.IDLE)
    cy.getByTestId('toolbox-policy-check').click()

    cy.getByTestId('toolbox-policy-check')
      .should('contain.text', 'Checking ...')
      .should('have.attr', 'data-status', PolicyDryRunStatus.RUNNING)

    cy.getByTestId('toolbox-policy-check')
      .should('have.text', 'Check')
      .should('have.attr', 'data-status', PolicyDryRunStatus.FAILURE)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolbarDryRun />, { wrapper: getPolicyPublishWrapper() })
    cy.getByTestId('toolbox-policy-check').click()

    cy.checkAccessibility()
  })
})
