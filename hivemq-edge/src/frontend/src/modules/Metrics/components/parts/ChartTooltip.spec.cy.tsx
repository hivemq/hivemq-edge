/// <reference types="cypress" />

import { DateTime } from 'luxon'
import ChartTooltip from './ChartTooltip.tsx'

describe('ChartTooltip', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    const mockTime = DateTime.fromMillis(1000)
    cy.injectAxe()
    cy.mountWithProviders(
      <ChartTooltip formattedValue={'the value'} color={'red.300'} date={mockTime} id={'the task'} />
    )

    const formatShortDate = new Intl.DateTimeFormat(navigator.language, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      second: 'numeric',
    })
    const shortDate = formatShortDate.format(mockTime.toJSDate())

    cy.getByTestId('chart-tooltip-id').should('contain.text', 'the task')
    cy.getByTestId('date-time-full').should('contain.text', shortDate)
    cy.getByTestId('chart-tooltip-value').should('contain.text', 'the value')

    cy.checkAccessibility()
    cy.percySnapshot('Component: ChartTooltip')
  })
})
