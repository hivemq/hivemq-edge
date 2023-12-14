/// <reference types="cypress" />

import { FC, PropsWithChildren } from 'react'
import { Box } from '@chakra-ui/react'

import { MOCK_METRIC_SAMPLE_ARRAY, MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import BarChart from './BarChart.tsx'
import { DateTime } from 'luxon'

const mockAriaLabel = 'aria-label'
const Wrapper: FC<PropsWithChildren> = ({ children }) => (
  <Box w={'100%'} h={400}>
    {children}
  </Box>
)

describe('BarChart', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <Wrapper>
        <BarChart
          h={'100%'}
          data={MOCK_METRIC_SAMPLE_ARRAY}
          metricName={MOCK_METRICS[0].name as string}
          aria-label={mockAriaLabel}
        />
      </Wrapper>,
    )

    const formatShortDate = new Intl.DateTimeFormat(navigator.language, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      second: 'numeric',
    })

    cy.get("[role='application']").should('have.attr', 'aria-label', mockAriaLabel)
    cy.get('text').should('contain.text', 'timestamp (s)')
    cy.get('svg > g > g > rect').eq(4).click()
    cy.getByTestId('bar-chart-tooltip')
      .should('contain.text', '[Forward] Publish success (count)')
      .should(
        'contain.text',
        formatShortDate.format(DateTime.fromISO(MOCK_METRIC_SAMPLE_ARRAY[5].sampleTime as string).toJSDate()),
      )
      .should('contain.text', '55000')
    cy.checkAccessibility()
    cy.percySnapshot('Component: BarChart')
  })
})
