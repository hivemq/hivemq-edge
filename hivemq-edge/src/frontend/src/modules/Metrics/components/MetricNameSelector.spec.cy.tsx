/// <reference types="cypress" />

import { MetricList } from '@/api/__generated__'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'

import MetricNameSelector from './MetricNameSelector.tsx'

describe('MetricNameSelector', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/metrics', { items: MOCK_METRICS } as MetricList).as('getMetrics')
  })

  it('should render the selector', () => {
    cy.mountWithProviders(
      <MetricNameSelector
        filter={mockBridgeId}
        onSubmit={cy.stub()}
        selectedMetrics={[MOCK_METRICS[0].name as string, MOCK_METRICS[0].name as string]}
      />
    )
  })
})
