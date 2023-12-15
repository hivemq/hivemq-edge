/// <reference types="cypress" />

import { MetricList } from '@/api/__generated__'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'

import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'
import Metrics from '@/modules/Metrics/Metrics.tsx'

describe('Metrics', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/metrics', { items: MOCK_METRICS } as MetricList).as('getMetrics')
  })

  it('should render the collapsible component', () => {
    cy.mountWithProviders(
      <Metrics nodeId={'bridge@bridge-id-01'} initMetrics={[]} id={mockBridgeId} type={NodeTypes.BRIDGE_NODE} />,
    )

    cy.getByTestId('metrics-toggle').should('have.attr', 'aria-expanded', 'false')
    cy.get('div#metrics-select-container').should('not.be.visible')

    cy.getByTestId('metrics-toggle').click()
    cy.get('div#metrics-select-container').should('be.visible')

    cy.getByTestId('metrics-toggle').click()
    cy.get('div#metrics-select-container').should('not.be.visible')
  })
})
