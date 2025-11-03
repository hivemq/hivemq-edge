/// <reference types="cypress" />

import PolicySummaryReport from './PolicySummaryReport.tsx'
import { PolicyDryRunStatus } from '@datahub/types.ts'

describe('PolicySummaryReport', () => {
  it('should render success alert', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.SUCCESS} />)

    cy.get('[data-testid="toolbox-policy-check-status"]').should('be.visible')
    cy.get('.chakra-alert').should('have.attr', 'data-status', 'success')
  })

  it('should render warning alert for failure status', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.FAILURE} />)

    cy.get('[data-testid="toolbox-policy-check-status"]').should('be.visible')
    cy.get('.chakra-alert').should('have.attr', 'data-status', 'warning')
  })

  it('should render error alert for idle status', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.IDLE} />)

    cy.get('[data-testid="toolbox-policy-check-status"]').should('be.visible')
    cy.get('.chakra-alert').should('have.attr', 'data-status', 'error')
  })

  it('should render error alert for undefined status', () => {
    cy.mountWithProviders(<PolicySummaryReport status={undefined} />)

    cy.get('[data-testid="toolbox-policy-check-status"]').should('be.visible')
    cy.get('.chakra-alert').should('have.attr', 'data-status', 'error')
  })

  it('should display alert icon', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.SUCCESS} />)

    cy.get('.chakra-alert__icon').should('be.visible')
  })

  it('should display alert title and description', () => {
    cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.SUCCESS} />)

    cy.get('[data-testid="toolbox-policy-check-status"]').within(() => {
      cy.get('.chakra-alert__title').should('exist')
      cy.get('.chakra-alert__desc').should('exist')
    })
  })

  describe('Accessibility', () => {
    it('should be accessible with success status', () => {
      cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.SUCCESS} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should be accessible with failure status', () => {
      cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.FAILURE} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should be accessible with idle status', () => {
      cy.mountWithProviders(<PolicySummaryReport status={PolicyDryRunStatus.IDLE} />)

      cy.injectAxe()
      cy.checkAccessibility()
    })
  })
})
