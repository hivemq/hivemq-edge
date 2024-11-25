import { expect } from 'vitest'
import { GenericObjectType, RJSFSchema } from '@rjsf/utils'

import { Adapter, ProtocolAdapter } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { TopicFilter } from '@/modules/Workspace/types.ts'

import { MOCK_PROTOCOL_MODBUS, MOCK_ADAPTER_MODBUS } from '@/__test-utils__/adapters/modbus.ts'
import { MOCK_PROTOCOL_SIMULATION, MOCK_ADAPTER_SIMULATION } from '@/__test-utils__/adapters/simulation.ts'
import { MOCK_PROTOCOL_OPC_UA, MOCK_ADAPTER_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_PROTOCOL_HTTP, MOCK_ADAPTER_HTTP } from '@/__test-utils__/adapters/http.ts'

import {
  discoverAdapterTopics,
  flattenObject,
  getBridgeTopics,
  getMainRootFromPath,
  getPropertiesFromPath,
  getTopicPaths,
  getValuesFromPath,
  mergeAllTopics,
} from './topics-utils.ts'
import { mockJSONSchema } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('getBridgeTopics', () => {
  it('should extract topics from a Bridge', async () => {
    const actual = getBridgeTopics(mockBridge)
    const expected: { local: TopicFilter[]; remote: TopicFilter[] } = {
      local: [{ topic: '#' }],
      remote: [{ topic: 'root/topic/ref/1' }],
    }

    expect(actual).toStrictEqual(expected)
  })

  it('should handle empty subscriptions', async () => {
    const actual = getBridgeTopics({ ...mockBridge, localSubscriptions: undefined, remoteSubscriptions: undefined })
    const expected: { local: TopicFilter[]; remote: TopicFilter[] } = {
      local: [],
      remote: [],
    }

    expect(actual).toStrictEqual(expected)
  })
})

interface Suite {
  protocol: NonNullable<ProtocolAdapter>
  formData?: GenericObjectType
  expectedPath: string[]
  expectedTopics: string[]
}

const validationSuite: Suite[] = [
  {
    protocol: MOCK_PROTOCOL_MODBUS,
    formData: MOCK_ADAPTER_MODBUS,
    expectedPath: ['subscriptions.*.destination'],
    expectedTopics: ['a/valid/topic/modbus/1'],
  },
  {
    protocol: MOCK_PROTOCOL_SIMULATION,
    formData: MOCK_ADAPTER_SIMULATION,
    expectedPath: ['subscriptions.*.destination'],
    expectedTopics: ['a/valid/topic/simulation/1'],
  },
  {
    protocol: MOCK_PROTOCOL_OPC_UA,
    formData: MOCK_ADAPTER_OPC_UA,
    expectedPath: ['subscriptions.*.mqtt-topic'],
    expectedTopics: ['a/valid/topic/opc-ua-client/1', 'a/valid/topic/opc-ua-client/2'],
  },
  {
    protocol: MOCK_PROTOCOL_HTTP,
    formData: MOCK_ADAPTER_HTTP,
    expectedPath: ['destination'],
    expectedTopics: ['a/valid/topic/http/1'],
  },

  {
    protocol: {
      ...MOCK_PROTOCOL_HTTP,
      id: 'http-without-subscription',
      configSchema: {
        ...MOCK_PROTOCOL_HTTP.configSchema,
        properties: {
          ...MOCK_PROTOCOL_HTTP.configSchema?.properties,
          destination: {
            ...MOCK_PROTOCOL_HTTP.configSchema?.properties.destination,
            format: 'something else',
          },
        },
      },
    },
    expectedPath: [],
    expectedTopics: [],
  },
  {
    protocol: {},
    expectedPath: [],
    expectedTopics: [],
  },
]

describe('flattenObject', () => {
  it('should work', () => {
    expect(flattenObject({})).toStrictEqual({})
    expect(flattenObject({ a: 1 })).toStrictEqual({
      a: 1,
    })
    expect(flattenObject({ a: 1, b: { c: 1 } })).toStrictEqual({
      a: 1,
      'b.c': 1,
    })
    expect(flattenObject({ a: 1, b: { c: 1, d: undefined, 'long-one': 1, 'with.dangerous-token': 1 } })).toStrictEqual({
      a: 1,
      'b.c': 1,
      'b.d': undefined,
      'b.long-one': 1,
      'b.with.dangerous-token': 1,
    })
  })
})

describe('discoverAdapterTopics', () => {
  it.each<Suite>(validationSuite)(
    `should return $expectedTopics.length with $protocol.id`,
    ({ protocol, formData, expectedPath, expectedTopics }) => {
      const paths = getTopicPaths(protocol?.configSchema as RJSFSchema)

      expect(paths).toEqual(expectedPath)

      expect(discoverAdapterTopics(protocol, formData?.config)).toEqual(expectedTopics)
    }
  )
})

describe('mergeAllTopics', () => {
  it('should extract every topics from all clients, bridges and adapters', async () => {
    const actual = mergeAllTopics(
      { items: [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_MODBUS] },
      [MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_MODBUS as Adapter],
      [mockBridge, mockBridge]
    )

    expect(actual).toStrictEqual([
      '#',
      'root/topic/ref/1',
      'a/valid/topic/opc-ua-client/1',
      'a/valid/topic/opc-ua-client/2',
      'a/valid/topic/modbus/1',
    ])
  })

  it('should extract every topics from all bridges', async () => {
    const actual = mergeAllTopics(undefined, undefined, [mockBridge, mockBridge])

    expect(actual).toStrictEqual(['#', 'root/topic/ref/1'])
  })

  it('should extract every topics from all  adapters', async () => {
    const actual = mergeAllTopics(
      { items: [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_MODBUS] },
      [MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_MODBUS as Adapter],
      undefined
    )

    expect(actual).toStrictEqual([
      'a/valid/topic/opc-ua-client/1',
      'a/valid/topic/opc-ua-client/2',
      'a/valid/topic/modbus/1',
    ])
  })

  it('should not extract any topic!', async () => {
    const actual = mergeAllTopics(undefined, undefined, undefined)

    expect(actual).toStrictEqual([])
  })
})

describe('getMainRootFromPath', () => {
  it('should work', () => {
    expect(getMainRootFromPath([])).toStrictEqual(undefined)
    expect(getMainRootFromPath(['root'])).toStrictEqual('root')
    expect(getMainRootFromPath(['root.'])).toStrictEqual('root')
    expect(getMainRootFromPath(['root-'])).toStrictEqual('root-')
    expect(getMainRootFromPath(['root.*.first.one'])).toStrictEqual('root')
    expect(getMainRootFromPath(['root.*.first.one', 'root2.*.next.one'])).toStrictEqual('root')
  })
})

describe('getValuesFromPath', () => {
  it('should work', () => {
    expect(getValuesFromPath('', mockJSONSchema)).toStrictEqual(undefined)
    expect(getValuesFromPath('test', mockJSONSchema)).toStrictEqual(undefined)
    expect(getValuesFromPath('test.still.wrong', mockJSONSchema)).toStrictEqual(undefined)
    expect(
      getValuesFromPath('properties.simulationToMqtt.properties.maxPollingErrorsBeforeRemoval', mockJSONSchema)
    ).toStrictEqual(
      expect.objectContaining({
        default: 10,
        description: 'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)',
        minimum: -1,
        title: 'Max. Polling Errors',
        type: 'integer',
      })
    )
  })
})

describe('getPropertiesFromPath', () => {
  it('should work', () => {
    expect(getPropertiesFromPath('test', undefined)).toStrictEqual(undefined)
    expect(getPropertiesFromPath('test', { type: 'string' })).toStrictEqual(
      expect.objectContaining({
        type: 'string',
      })
    )
    expect(getPropertiesFromPath('test.*', { type: 'string' })).toStrictEqual(undefined)
    expect(getPropertiesFromPath('simulationToMqtt.simulationToMqttMappings.*.items', mockJSONSchema)).toStrictEqual(
      expect.objectContaining({
        mqttTopic: {
          description: 'The topic to publish data on',
          format: 'mqtt-topic',
          title: 'Destination MQTT Topic',
          type: 'string',
        },
        mqttQos: {
          default: 0,
          description: 'MQTT Quality of Service level',
          maximum: 2,
          minimum: 0,
          title: 'MQTT QoS',
          type: 'integer',
        },
      })
    )
  })
})
