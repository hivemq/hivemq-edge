import { useNodes } from 'reactflow'
import { stratify } from 'd3-hierarchy'

import { Adapter, Bridge } from '@/api/__generated__'

import { NodeTypes, TopicFilter } from '../types.ts'
import { getAdapterTopics, getBridgeTopics } from '../utils/topics-utils.ts'

const useGetWorkspaceTopics = () => {
  const nodes = useNodes()

  const allTopics: TopicFilter[] = []
  nodes.forEach((n) => {
    if (n.type === NodeTypes.ADAPTER_NODE) {
      const topics = getAdapterTopics(n.data as Adapter)
      allTopics.push(...topics)
    } else if (n.type === NodeTypes.BRIDGE_NODE) {
      const { local, remote } = getBridgeTopics(n.data as Bridge)
      allTopics.push(...local, ...remote)
    }
  })

  if (!allTopics.length) return []
  return stratify<TopicFilter>()
    .path((d) => d.topic)(allTopics)
    .count()
}

export default useGetWorkspaceTopics
