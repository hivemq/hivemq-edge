/// <reference types="cypress" />

import { DateTime } from 'luxon'
import { MOCK_METRIC_SAMPLE, MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import Sample from './Sample.tsx'

describe('Sample', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    let increment = 0
    cy.intercept(`/api/v1/metrics/**`, (req) => {
      increment++
      req.reply({
        sampleTime: DateTime.fromISO(MOCK_METRIC_SAMPLE.sampleTime as string)
          .plus({ second: 15 * increment })
          .toISO(),
        value: (MOCK_METRIC_SAMPLE.value as number) + increment * 1000,
      })
    }).as('getSample')
  })

  it('should render the bridge component', () => {
    cy.mountWithProviders(<Sample metricName={MOCK_METRICS[0].name} onClose={cy.stub()} />)

    cy.get('dd').should('contain.text', '51,000')

    // This is not a great solution. Use clock ?
    cy.get('dd', { timeout: 12000 }).should('contain.text', '52,000')
  })
})
