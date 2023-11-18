import { describe, expect } from 'vitest'

import { extractMetricInfo, MetricInfo } from './metrics-name.utils.ts'

interface MetricInfoSuite {
  metricName: string
  expected: MetricInfo
}

describe('extractMetricInfo', () => {
  test.each<MetricInfoSuite>([
    {
      metricName: 'com.hivemq.edge.protocol-adapters.modbus.fff.connection.success.count',
      expected: {
        device: 'protocol-adapters',
        id: 'fff',
        suffix: 'connection.success.count',
        type: 'modbus',
      },
    },
  ])('should returns $expected.suffix with metricName', ({ metricName, expected }) => {
    expect(extractMetricInfo(metricName)).toStrictEqual(expected)
  })
})
