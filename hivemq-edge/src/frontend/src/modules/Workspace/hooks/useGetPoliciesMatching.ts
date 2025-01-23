import type { Node } from 'reactflow'
import type { Bridge, DataPolicy } from '@/api/__generated__'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import type { Group } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { discoverAdapterTopics, getBridgeTopics } from '@/modules/Workspace/utils/topics-utils.ts'
import mqttTopicMatch from 'mqtt-match'

import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'

//TODO[NVL] return isLoading, isError and maybe error?
export const useGetPoliciesMatching = (id: string) => {
  const hasDataHub = useGetCapability(CAPABILITY.DATAHUB)
  const { isLoading: isDataLoading, data: dataPolicies, isError: isDataError } = useGetAllDataPolicies()
  const { nodes: workspaceNodes } = useWorkspaceStore()
  const { data: protocols } = useGetAdapterTypes()

  if (!hasDataHub || isDataLoading || isDataError) return undefined

  const sourceNode = workspaceNodes.find((node) => node.id === id)
  if (!sourceNode) return []

  if (sourceNode.type === NodeTypes.BRIDGE_NODE) {
    const bridge = sourceNode.data as Bridge
    const { remote } = getBridgeTopics(bridge)
    const allTopics = remote.map((topic) => topic.topic)

    return dataPolicies?.items?.filter((policy) =>
      allTopics.some((topic) => mqttTopicMatch(policy.matching.topicFilter, topic))
    )
  }

  const getPoliciesForAdapter = (node: Node): DataPolicy[] | undefined => {
    const adapterProtocol = protocols?.items?.find((e) => e.id === node.data.type)
    if (!adapterProtocol) return undefined

    const allTopics = discoverAdapterTopics(adapterProtocol, node.data.config)

    return dataPolicies?.items?.filter((policy) =>
      allTopics.some((topic) => mqttTopicMatch(policy.matching.topicFilter, topic))
    )
  }

  if (sourceNode.type === NodeTypes.ADAPTER_NODE) {
    return getPoliciesForAdapter(sourceNode)
  }

  if (sourceNode.type === NodeTypes.CLUSTER_NODE) {
    const group = sourceNode.data as Group

    const policies = group.childrenNodeIds.reduce((acc: DataPolicy[], curr: string) => {
      const node = workspaceNodes.find((n: Node) => n.id === curr)
      if (node) {
        const policy = getPoliciesForAdapter(node)
        if (policy) {
          acc.push(...policy)
        }
      }
      return acc
    }, [])

    return Array.from(new Set(policies))
  }

  return undefined
}
