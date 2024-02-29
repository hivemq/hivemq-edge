/// <reference types="cypress" />

import { DateTime } from 'luxon'
import { MOCK_METRIC_SAMPLE, MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import { extractMetricInfo, MetricInfo } from '@/modules/Metrics/utils/metrics-name.utils.ts'
import { DataPoint } from '@/api/__generated__'

import SampleRenderer from './SampleRenderer.tsx'

const mockMetricInfo: MetricInfo = extractMetricInfo(MOCK_METRICS[0].name as string)

describe('SampleRenderer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the Stat component with no data point', () => {
    cy.mountWithProviders(<SampleRenderer metricInfo={mockMetricInfo} series={[]} isLoading={false} />)
    cy.get('dd').should('contain.text', '-')
    cy.getByTestId('metric-change').should('not.exist')
  })

  it('should render the Stat component with a single data point', () => {
    const mockSeries: DataPoint[] = [MOCK_METRIC_SAMPLE]

    cy.mountWithProviders(<SampleRenderer metricInfo={mockMetricInfo} series={mockSeries} isLoading={false} />)
    cy.get('dd').should('contain.text', '50,000')
    cy.getByTestId('metric-change').should('not.exist')
  })

  it('should render the Stat component with increased data points', () => {
    const mockSeries: DataPoint[] = [
      {
        sampleTime: DateTime.fromISO(MOCK_METRIC_SAMPLE.sampleTime as string)
          .plus({ second: 15 })
          .toISO(),
        value: (MOCK_METRIC_SAMPLE.value as number) + 10000,
      },
      MOCK_METRIC_SAMPLE,
    ]

    cy.mountWithProviders(<SampleRenderer metricInfo={mockMetricInfo} series={mockSeries} isLoading={false} />)
    cy.get('dd').should('contain.text', '60,000')
    cy.getByTestId('metric-change').should('contain.text', 'increased by10,000')
  })

  it('should render the Stat component with decreased data points', () => {
    const mockSeries: DataPoint[] = [
      {
        sampleTime: DateTime.fromISO(MOCK_METRIC_SAMPLE.sampleTime as string)
          .minus({ second: 15 })
          .toISO(),
        value: (MOCK_METRIC_SAMPLE.value as number) - 10000,
      },
      MOCK_METRIC_SAMPLE,
    ]

    cy.mountWithProviders(<SampleRenderer metricInfo={mockMetricInfo} series={mockSeries} isLoading={false} />)
    cy.get('dd').should('contain.text', '40,000')
    cy.getByTestId('metric-change').should('contain.text', 'decreased by10,000')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    const mockSeries: DataPoint[] = [
      {
        sampleTime: DateTime.fromISO(MOCK_METRIC_SAMPLE.sampleTime as string)
          .plus({ second: 15 })
          .toISO(),
        value: (MOCK_METRIC_SAMPLE.value as number) + 10000,
      },
      MOCK_METRIC_SAMPLE,
    ]
    cy.mountWithProviders(<SampleRenderer metricInfo={mockMetricInfo} series={mockSeries} isLoading={false} />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: SampleRenderer')
  })
})
