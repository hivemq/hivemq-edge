/// <reference types="cypress" />

import type { MetricList } from '@/api/__generated__'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'

import MetricsContainer from '@/modules/Metrics/MetricsContainer.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'

describe('MetricsContainer', () => {
  beforeEach(() => {
    cy.viewport(600, 800)
    cy.intercept('/api/v1/metrics/**/*', { statusCode: 404, log: false })
    cy.intercept('/api/v1/metrics', { items: MOCK_METRICS }).as('getMetrics')
  })

  it('should render the collapsible component', () => {
    cy.mountWithProviders(
      <MetricsContainer
        nodeId="bridge@bridge-id-01"
        initMetrics={[MOCK_METRICS[0].name as string, MOCK_METRICS[1].name as string]}
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

  it('should render message when no metrics', () => {
    cy.intercept('/api/v1/metrics**', { items: [] } as MetricList).as('getMetrics')

    cy.mountWithProviders(
      <MetricsContainer
        nodeId="bridge@bridge-id-02"
        initMetrics={[]}
        filters={[
          {
            id: 'bridge@bridge-id-02',
            type: 'com.hivemq.edge.bridge',
          },
        ]}
        type={NodeTypes.BRIDGE_NODE}
      />
    )

    cy.wait('@getMetrics')
    cy.getByTestId('metrics-toggle').should('be.disabled')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'There is no metrics available for this entity')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <MetricsContainer
        nodeId="bridge@bridge-id-01"
        initMetrics={[MOCK_METRICS[0].name as string, MOCK_METRICS[1].name as string]}
        filters={[
          {
            id: mockBridgeId,
            type: 'com.hivemq.edge.bridge',
          },
        ]}
        type={NodeTypes.BRIDGE_NODE}
      />
    )
    cy.checkAccessibility()
  })
})
