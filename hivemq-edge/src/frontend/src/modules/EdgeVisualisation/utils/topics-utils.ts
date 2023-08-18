import { Adapter, Bridge, BridgeSubscription } from '@/api/__generated__'

import { OpcUaClient } from '../types/opc-ua-client'
import { Modbus } from '../types/modbus'
import { Simulation } from '../types/simulation'
import { TopicFilter } from '../types.ts'

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
