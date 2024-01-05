/// <reference types="cypress" />

import { FC, PropsWithChildren } from 'react'
import { Box } from '@chakra-ui/react'

import { MOCK_METRIC_SAMPLE_ARRAY, MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import LineChart from './LineChart.tsx'

const mockAriaLabel = 'aria-label'
const Wrapper: FC<PropsWithChildren> = ({ children }) => (
  <Box w={'100%'} h={400}>
    {children}
  </Box>
)

describe('LineChart', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <Wrapper>
        <LineChart
          h={'100%'}
          data={MOCK_METRIC_SAMPLE_ARRAY}
          metricName={MOCK_METRICS[0].name as string}
          aria-label={mockAriaLabel}
        />
      </Wrapper>
    )

    cy.get("[role='application']").should('have.attr', 'aria-label', mockAriaLabel)
    cy.get('text').should('contain.text', 'timestamp (seconds ago)')

    cy.checkAccessibility()
    cy.percySnapshot('Component: LineChart')
  })
})
