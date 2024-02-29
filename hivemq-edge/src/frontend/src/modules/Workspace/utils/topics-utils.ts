import { Adapter, Bridge, BridgeSubscription, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { GenericObjectType, RJSFSchema } from '@rjsf/utils'

import { CustomFormat } from '@/api/types/json-schema.ts'

import { TopicFilter } from '../types.ts'

const TOPIC_PATH_ITEMS_TOKEN = '*'

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
  // TODO[138] review nullable for protocol
  const paths = getTopicPaths(protocol?.configSchema || {})
  const topics: string[] = []

  paths.forEach((path) => {
    const gg = getTopicsFromPath(path, instance)
    topics.push(...gg)
  })

  return topics
}

export const mergeAllTopics = (
  types: ProtocolAdaptersList | undefined,
  adapters: Adapter[] | undefined,
  bridges: Bridge[] | undefined
) => {
  const data: string[] = []
  if (bridges) {
    const bridgeTopics = bridges.reduce<string[]>((acc, cur) => {
      const { local, remote } = getBridgeTopics(cur)
      acc.push(...local.map((e) => e.topic))
      acc.push(...remote.map((e) => e.topic))
      return acc
    }, [])
    data.push(...bridgeTopics)
  }
  if (adapters) {
    const adapterTopics = adapters.reduce<string[]>((acc, cur) => {
      const type = types?.items?.find((e) => e.id === cur.type)
      if (!type) return acc
      const topics = discoverAdapterTopics(type, cur.config as GenericObjectType)
      acc.push(...topics)
      // const topics = getAdapterTopics(cur)
      // acc.push(...topics.map((e) => e.topic))
      return acc
    }, [])
    data.push(...adapterTopics)
  }

  return Array.from(new Set(data))
}
