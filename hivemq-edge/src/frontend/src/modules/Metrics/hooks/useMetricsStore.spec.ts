import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'

import useMetricsStore, {
  MetricDefinitionSpec,
  MetricDefinitionStore,
} from '@/modules/Metrics/hooks/useMetricsStore.ts'
import { ChartType } from '@/modules/Metrics/types.ts'

describe('useMetricsStore', () => {
  beforeEach(() => {
    const { result } = renderHook<MetricDefinitionStore, unknown>(useMetricsStore)
    act(() => {
      result.current.reset()
    })
  })

  it('should start with an empty store', async () => {
    const { result } = renderHook<MetricDefinitionStore, unknown>(useMetricsStore)
    const { metrics } = result.current

    expect(metrics).toHaveLength(0)
  })

  it('should add a metric to the store', async () => {
    const { result } = renderHook<MetricDefinitionStore, unknown>(useMetricsStore)
    const { metrics } = result.current

    expect(metrics).toHaveLength(0)

    act(() => {
      const { addMetrics } = result.current
      addMetrics('nodeId', 'metricNAme', ChartType.SAMPLE)
    })

    expect(result.current.metrics).toHaveLength(1)
  })

  it('should remove a metric to the store', async () => {
    const { result } = renderHook<MetricDefinitionStore, unknown>(useMetricsStore)
    const { metrics } = result.current

    expect(metrics).toHaveLength(0)

    act(() => {
      const { addMetrics } = result.current
      addMetrics('nodeId', 'metricName1', ChartType.SAMPLE)
      addMetrics('nodeId', 'metricName2', ChartType.SAMPLE)
      addMetrics('anotherNodeId', 'metricName1', ChartType.SAMPLE)
    })

    expect(result.current.metrics).toHaveLength(3)

    act(() => {
      const { removeMetrics } = result.current
      removeMetrics('nodeId', 'metricName1')
    })

    expect(result.current.metrics).toStrictEqual<MetricDefinitionSpec[]>([
      {
        chart: ChartType.SAMPLE,
        metrics: 'metricName2',
        source: 'nodeId',
      },
      {
        chart: ChartType.SAMPLE,
        metrics: 'metricName1',
        source: 'anotherNodeId',
      },
    ])
  })

  it('should return all the metrics for a node', async () => {
    const { result } = renderHook<MetricDefinitionStore, unknown>(useMetricsStore)
    const { metrics } = result.current

    expect(metrics).toHaveLength(0)

    act(() => {
      const { addMetrics } = result.current
      addMetrics('nodeId', 'metricName1', ChartType.SAMPLE)
      addMetrics('nodeId', 'metricName2', ChartType.SAMPLE)
      addMetrics('anotherNodeId', 'metricName1', ChartType.SAMPLE)
    })

    expect(result.current.metrics).toHaveLength(3)

    act(() => {
      const { getMetricsFor } = result.current
      const res = getMetricsFor('nodeId')
      expect(res).toStrictEqual<MetricDefinitionSpec[]>([
        {
          chart: ChartType.SAMPLE,
          metrics: 'metricName1',
          source: 'nodeId',
        },
        {
          chart: ChartType.SAMPLE,
          metrics: 'metricName2',
          source: 'nodeId',
        },
      ])
    })
  })

  it('should reset the store', async () => {
    const { result } = renderHook<MetricDefinitionStore, unknown>(useMetricsStore)
    const { metrics } = result.current

    expect(metrics).toHaveLength(0)

    act(() => {
      const { addMetrics } = result.current
      addMetrics('nodeId', 'metricName1', ChartType.SAMPLE)
      addMetrics('nodeId', 'metricName2', ChartType.SAMPLE)
      addMetrics('anotherNodeId', 'metricName1', ChartType.SAMPLE)
    })

    expect(result.current.metrics).toHaveLength(3)

    act(() => {
      const { reset } = result.current
      reset()
    })

    expect(result.current.metrics).toHaveLength(0)
  })
})
