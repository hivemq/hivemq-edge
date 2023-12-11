/// <reference types="cypress" />

import { MOCK_METRIC_SAMPLE, MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import { ChartType } from '@/modules/Metrics/types.ts'

import ChartContainer from './ChartContainer.tsx'

describe('ChartContainer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept(`/api/v1/metrics/**`, MOCK_METRIC_SAMPLE)
  })

  it('should render the chart container', () => {
    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(
      <ChartContainer chartType={ChartType.LINE_CHART} onClose={onClose} metricName={MOCK_METRICS[0].name} />
    )

    cy.getByTestId('chart-container').should('exist')
    cy.getByTestId('chart-container-toolbar').should('exist')
    cy.getByAriaLabel('Remove from panel').should('be.visible')
    cy.getByAriaLabel('Collapse the chart').should('be.visible')
    cy.getByAriaLabel('Copy to the clipboard').should('be.visible')

    cy.getByTestId('metrics-remove').click()
    cy.get('@onClose').should('have.been.calledOnce')

    // cy.get('dd').should('contain.text', '50,000')
    //
    // cy.getByTestId('metrics-remove').click()
    // cy.get('@onClose').should('have.been.calledOnce')
    //
    // cy.getByTestId('metrics-copy').click()
  })

  it('should not allow editing', () => {
    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(
      <ChartContainer
        chartType={ChartType.LINE_CHART}
        onClose={onClose}
        metricName={MOCK_METRICS[0].name}
        canEdit={false}
      />
    )

    cy.getByTestId('metrics-remove').should('not.exist')
  })
})
