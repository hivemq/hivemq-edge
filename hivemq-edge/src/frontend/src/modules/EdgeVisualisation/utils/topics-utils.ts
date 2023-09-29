import { Adapter, Bridge, BridgeSubscription, ProtocolAdapter } from '@/api/__generated__'
import { GenericObjectType, RJSFSchema } from '@rjsf/utils'

import { OpcUaClient } from '@/api/__generated__/adapters/opc-ua-client'
import { Modbus } from '@/api/__generated__/adapters/modbus'
import { Simulation } from '@/api/__generated__/adapters/simulation'
import { CustomFormat } from '@/api/types/json-schema.ts'

import { TopicFilter } from '../types.ts'

const TOPIC_PATH_ITEMS_TOKEN = '*'

/**
 * @deprecated switch to discoverAdapterTopics
 * @param adapter
 * @see discoverAdapterTopics
 */
/* istanbul ignore next -- @preserve */
export const getAdapterTopics = (adapter: Adapter): TopicFilter[] => {
  if (adapter.type === 'opc-ua-client') {
    const { subscriptions } = adapter.config as unknown as OpcUaClient
    return subscriptions?.map((e) => ({ topic: e['mqtt-topic'] })) || []
  }

  if (adapter.type === 'modbus') {
    const { subscriptions } = adapter.config as unknown as Modbus
    return subscriptions?.map((e) => ({ topic: e.destination })) || []
  }

  if (adapter.type === 'simulation') {
    const { subscriptions } = adapter.config as unknown as Simulation
    return subscriptions?.map((e) => ({ topic: e.destination })) || []
  }

  return []
}

const subsToTopics = (subs: BridgeSubscription[] | undefined): TopicFilter[] => {
  return (
    subs?.reduce<TopicFilter[]>((acc, cur) => {
      const topics = cur.filters.map<TopicFilter>((w) => ({ topic: w }))
      acc.push(...topics)
      return acc
    }, []) || []
  )
}

export const getBridgeTopics = (bridge: Bridge): { local: TopicFilter[]; remote: TopicFilter[] } => {
  return {
    local: subsToTopics(bridge.localSubscriptions),
    remote: subsToTopics(bridge.remoteSubscriptions),
  }
}

export const flattenObject = (input: RJSFSchema, root = '') => {
  let result: Record<string, unknown> = {}
  for (const key in input) {
    const newKey = root ? `${root}.${key}` : key
    if (typeof input[key] === 'object' && !Array.isArray(input[key])) {
      result = { ...result, ...flattenObject(input[key], newKey) }
    } else {
      result[newKey] = input[key]
    }
  }
  return result
}

export const getTopicPaths = (configSchema: RJSFSchema) => {
  const flattenSchema = flattenObject(configSchema)
  return (
    Object.entries(flattenSchema)
      // Only interested in topics, internally defined by the string format `format: 'mqtt-topic'`
      .filter(([k, v]) => k.endsWith('format') && v === CustomFormat.MQTT_TOPIC)
      .map(([path]) =>
        path
          // The root of the path will always be "properties" [?]
          .replace('properties.', '')
          // The leaf of the path will always be "format"
          .replace('.format', '')
          // A `type: 'array'` property will have a `items: { properties: {}}` pattern [?]
          .replace(/items\.properties/gi, TOPIC_PATH_ITEMS_TOKEN)
      )
  )
}

const getTopicsFromPath = (path: string, instance: RJSFSchema): string[] => {
  /* istanbul ignore next -- @preserve */
  if (!path.length) {
    console.log('Warning! Is this really happening?')
    return []
  }
  const [property, ...rest] = path.split('.')

  if (!rest.length) return [instance?.[property]]
  if (property === TOPIC_PATH_ITEMS_TOKEN) {
    const res: string[] = []

    for (const item of instance as RJSFSchema[]) {
      const gg = getTopicsFromPath(rest.join('.'), item)
      res.push(...gg)
    }
    return res
  }
  return getTopicsFromPath(rest.join('.'), instance?.[property])
}

export const discoverAdapterTopics = (protocol: ProtocolAdapter, instance: GenericObjectType): string[] => {
  const paths = getTopicPaths(protocol.configSchema as RJSFSchema)
  const topics: string[] = []

  paths.forEach((path) => {
    const gg = getTopicsFromPath(path, instance)
    topics.push(...gg)
  })

  return topics
}

export const mergeAllTopics = (adapters: Adapter[] | undefined, bridges: Bridge[] | undefined) => {
  const data: string[] = []
  if (bridges) {
    const gg = bridges.reduce<string[]>((acc, cur) => {
      const { local, remote } = getBridgeTopics(cur)
      acc.push(...local.map((e) => e.topic))
      acc.push(...remote.map((e) => e.topic))
      return acc
    }, [])
    data.push(...gg)
  }
  if (adapters) {
    const gg = adapters.reduce<string[]>((acc, cur) => {
      const topics = getAdapterTopics(cur)
      acc.push(...topics.map((e) => e.topic))
      return acc
    }, [])
    data.push(...gg)
  }

  return Array.from(new Set(data))
}
