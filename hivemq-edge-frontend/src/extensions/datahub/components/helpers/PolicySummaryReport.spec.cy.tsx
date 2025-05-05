import { PolicyDryRunStatus } from '@datahub/types.ts'
import PolicySummaryReport from '@datahub/components/helpers/PolicySummaryReport.tsx'

describe('PolicySummaryReport', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.SUCCESS} />)

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'success')
      .should('contain.text', 'The policy is fully valid to run on your topology.')
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.FAILURE} />)

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'warning')
      .should('contain.text', 'The policy is not completely valid and will not successfully run on your topology.')
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<PolicySummaryReport status={undefined} />)

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'No information to display')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.SUCCESS} />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: PolicySummaryReport')
  })
})
