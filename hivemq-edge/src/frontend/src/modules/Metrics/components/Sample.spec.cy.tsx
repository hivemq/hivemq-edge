/// <reference types="cypress" />

import { MOCK_METRIC_SAMPLE, MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import Sample from './Sample.tsx'

describe('Sample', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept(`/api/v1/metrics/**`, MOCK_METRIC_SAMPLE).as('getSample')
  })

  it('should render the bridge component', () => {
    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(<Sample metricName={MOCK_METRICS[0].name} onClose={onClose} />)

    cy.get('dd').should('contain.text', '50,000')

    cy.getByTestId('metrics-remove').click()
    cy.get('@onClose').should('have.been.calledOnce')

    cy.getByTestId('metrics-copy').click()
    cy.get('@onClipboardCopy').should(
      'have.been.calledWith',
      'com.hivemq.edge.bridge.bridge-id-01.forward.publish.count',
      '2023-11-18T00:00:00Z',
      50000
    )
  })
})
