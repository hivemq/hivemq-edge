import { GenericObjectType, RJSFSchema } from '@rjsf/utils'
import { JSONSchema7 } from 'json-schema'
import { stratify } from 'd3-hierarchy'

import { Adapter, Bridge, BridgeSubscription, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { CustomFormat } from '@/api/types/json-schema.ts'
import { BrokerClient } from '@/api/types/api-broker-client.ts'
import { TopicFilter, type TopicTreeMetadata } from '../types.ts'

export const TOPIC_PATH_ITEMS_TOKEN = '*'

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

export const getMainRootFromPath = (paths: string[]): string | undefined => {
  const firstPath = paths.shift()
  if (!firstPath) return undefined

  const root = firstPath.split('.').shift()
  if (!root) return undefined
  return root
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

export const getPropertiesFromPath = (path: string, instance: JSONSchema7 | undefined): JSONSchema7 | undefined => {
  const [property, ...rest] = path.split('.')

  if (!instance) return undefined
  if (!rest.length) {
    // TODO[NVL] should we test that the path is a property of instance?
    return instance
  }

  if (property === TOPIC_PATH_ITEMS_TOKEN) {
    const { properties } = instance.items as JSONSchema7
    return getPropertiesFromPath(rest.join('.'), properties)
  }
  const { properties } = instance as JSONSchema7
  return getPropertiesFromPath(rest.join('.'), properties?.[property] as JSONSchema7)
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

    /* istanbul ignore else -- @preserve */
    if (Array.isArray(instance)) {
      for (const item of instance as RJSFSchema[]) {
        const topicsFromPath = getTopicsFromPath(rest.join('.'), item)
        res.push(...topicsFromPath)
      }
    } else {
      const topicsFromPath = getTopicsFromPath(rest.join('.'), instance as RJSFSchema)
      res.push(...topicsFromPath)
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
    const topicsFromPath = getTopicsFromPath(path, instance)
    topics.push(...topicsFromPath)
  })

  return topics
}

export const mergeAllTopics = (
  types: ProtocolAdaptersList | undefined,
  adapters: Adapter[] | undefined,
  bridges: Bridge[] | undefined,
  clients: BrokerClient[] | undefined,
  withOrigin = false
) => {
  const data: string[] = []
  if (bridges) {
    const bridgeTopics = bridges.reduce<string[]>((acc, cur) => {
      const { local, remote } = getBridgeTopics(cur)
      acc.push(...local.map((topicFilter) => topicFilter.topic))
      acc.push(...remote.map((topicFilter) => topicFilter.topic))
      // TODO[25055] The  data structure needs refactoring
      return withOrigin ? acc.map((e) => `${cur.id}/${e}`) : acc
    }, [])
    data.push(...bridgeTopics)
  }
  if (adapters) {
    const adapterTopics = adapters.reduce<string[]>((acc, cur) => {
      const type = types?.items?.find((protocolAdapter) => protocolAdapter.id === cur.type)
      /* istanbul ignore next -- @preserve */
      if (!type) return acc
      const topics = discoverAdapterTopics(type, cur.config as GenericObjectType)
      acc.push(...(withOrigin ? topics.map((e) => `${e} @ ${cur.id}`) : topics))
      return acc
    }, [])
    data.push(...adapterTopics)
  }
  if (clients) {
    for (const client of clients) {
      const subs = client.config.subscriptions?.map((subs) => subs.destination)
      data.push(...(subs || []))
    }
  }

  return Array.from(new Set(data))
}

export const toTreeMetadata = (
  topics: string[],
  unsPrefix?: string,
  sum?: (t: string) => number
): TopicTreeMetadata[] => {
  return topics.map<TopicTreeMetadata>((topic) => {
    let label = topic
    // TODO This might not be the only wildcard to take care ofß
    if (unsPrefix && !topic.includes('#')) {
      label = unsPrefix.concat(topic)
    }
    return { label: label, count: sum ? sum(topic) : 1 }
  })
}

export const stratifyTopicTree = (topics: TopicTreeMetadata[]) => {
  return stratify<TopicTreeMetadata>().path((d) => d.label)(topics)
}
