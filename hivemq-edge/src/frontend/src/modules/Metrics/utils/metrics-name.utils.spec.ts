import { describe, expect } from 'vitest'

import { extractMetricInfo, MetricInfo } from './metrics-name.utils.ts'
import {
  MOCK_METRIC_ADAPTER,
  MOCK_METRIC_BRIDGE,
  MOCK_METRIC_MESSAGE,
  MOCK_METRICS,
} from '@/api/hooks/useGetMetrics/__handlers__'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'

interface MetricInfoSuite {
  metricName: string
  expected: MetricInfo
}

describe('extractMetricInfo', () => {
  test.each<MetricInfoSuite>([
    {
      metricName: MOCK_METRIC_ADAPTER,
      expected: {
        device: 'protocol-adapters',
        id: 'my-adapter',
        suffix: 'connection.failed.count',
        type: 'opc-ua-client',
      },
    },
    {
      metricName: MOCK_METRIC_BRIDGE,
      expected: {
        device: 'bridge',
        id: mockBridgeId,
        suffix: 'forward.publish.count',
      },
    },
    {
      metricName: MOCK_METRIC_MESSAGE,
      expected: {
        device: 'messages',
        suffix: 'dropped.count',
      },
    },
    {
      metricName: MOCK_METRICS[20].name as string,
      expected: {
        device: 'networking',
        suffix: 'bytes.read.total',
      },
    },
  ])('should returns $expected.suffix with metricName', ({ metricName, expected }) => {
    expect(extractMetricInfo(metricName)).toStrictEqual(expected)
  })
})
