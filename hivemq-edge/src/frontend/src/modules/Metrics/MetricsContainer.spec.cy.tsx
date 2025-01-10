/// <reference types="cypress" />

import { MetricList } from '@/api/__generated__'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'

import MetricsContainer from '@/modules/Metrics/MetricsContainer.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'

describe('MetricsContainer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/metrics', { items: MOCK_METRICS } as MetricList)
  })

  it('should render the collapsible component', () => {
    cy.mountWithProviders(
      <MetricsContainer
        nodeId="bridge@bridge-id-01"
        initMetrics={[]}
        filters={[
          {
            id: mockBridgeId,
            type: 'com.hivemq.edge.bridge',
          },
        ]}
        type={NodeTypes.BRIDGE_NODE}
      />
    )

    cy.getByTestId('metrics-toggle').should('have.attr', 'aria-expanded', 'false')
    cy.get('div#metrics-select-container').should('not.be.visible')

    cy.getByTestId('metrics-toggle').click()
    cy.get('div#metrics-select-container').should('be.visible')

    cy.getByTestId('metrics-toggle').click()
    cy.get('div#metrics-select-container').should('not.be.visible')
  })
})
