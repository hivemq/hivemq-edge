/// <reference types="cypress" />

import { DateTime } from 'luxon'
import ChartTooltip from './ChartTooltip.tsx'

describe('ChartTooltip', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <ChartTooltip formattedValue={'the value'} color={'red.300'} date={DateTime.fromMillis(1000)} id={'the task'} />
    )

    cy.getByTestId('chart-tooltip-id').should('contain.text', 'the task')
    cy.getByTestId('date-time-full').should('contain.text', '1 Jan, 01:00:01')
    cy.getByTestId('chart-tooltip-value').should('contain.text', 'the value')

    cy.checkAccessibility()
    cy.percySnapshot('Component: ChartTooltip')
  })
})
