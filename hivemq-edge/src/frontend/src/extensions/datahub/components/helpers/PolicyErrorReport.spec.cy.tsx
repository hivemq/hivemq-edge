/// <reference types="cypress" />

import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { DataHubNodeType } from '@datahub/types.ts'

describe('PolicyErrorReport', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    const onFitView = cy.stub().as('onFitView')
    const onOpenConfig = cy.stub().as('onOpenConfig')

    cy.mountWithProviders(
      <PolicyErrorReport
        onFitView={onFitView}
        onOpenConfig={onOpenConfig}
        errors={[
          { title: 'wrong type', status: 404, type: 'error-type', id: '123', detail: 'the description' },
          {
            title: DataHubNodeType.BEHAVIOR_POLICY,
            status: 404,
            type: 'error-type',
            id: '456',
            detail: 'the description',
          },
        ]}
      />
    )
    cy.get('h2').first().should('contain.text', 'Unknown type')
    cy.getByTestId('report-error-fitView').first().should('not.be.visible')

    cy.get('h2').first().click()
    cy.getByTestId('report-error-fitView').first().should('be.visible')

    cy.getByTestId('report-error-fitView').first().click()
    cy.get('@onFitView').should('have.been.calledWith', '123')

    cy.getByTestId('report-error-config').first().click()
    cy.get('@onOpenConfig').should('have.been.calledWith', '123')
  })
})
