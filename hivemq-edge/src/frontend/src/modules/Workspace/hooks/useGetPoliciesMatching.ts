import { Node } from 'reactflow'
import { Bridge, DataPolicy } from '@/api/__generated__'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.tsx'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { Group, NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { discoverAdapterTopics, getBridgeTopics } from '@/modules/Workspace/utils/topics-utils.ts'

import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'

//TODO[NVL] return isLoading, isError and maybe error?
export const useGetPoliciesMatching = (id: string) => {
  const hasDataHub = useGetCapability(CAPABILITY.DATAHUB)
  const { isLoading: isDataLoading, data: dataPolicies, isError: isDataError } = useGetAllDataPolicies()
  const { nodes: workspaceNodes } = useWorkspaceStore()
  const { data: protocols } = useGetAdapterTypes()

  if (!hasDataHub) return undefined
  if (isDataLoading) return undefined
  if (isDataError) return undefined

  const sourceNode = workspaceNodes.find((node) => node.id === id)
  if (!sourceNode) return []

  if (sourceNode.type === NodeTypes.BRIDGE_NODE) {
    const bridge = sourceNode.data as Bridge
    const { remote } = getBridgeTopics(bridge)
    const allTopics = remote.map((topic) => topic.topic)

    // TODO[NVL] It cannot be allTopics.includes! This is a topic filter matching
    return dataPolicies.items?.filter((policy) => allTopics.includes(policy.matching.topicFilter))
  }

  const getPoliciesForAdapter = (node: Node): DataPolicy[] | undefined => {
    const adapterProtocol = protocols?.items?.find((e) => e.id === node.data.type)
    if (!adapterProtocol) return undefined

    const allTopics = discoverAdapterTopics(adapterProtocol, node.data.config)
      .map((e) => ({ topic: e }))
      .map((e) => e.topic)

    // TODO[NVL] It cannot be allTopics.includes! This is a topic filter matching
    return dataPolicies.items?.filter((policy) => allTopics.includes(policy.matching.topicFilter))
  }

  if (sourceNode.type === NodeTypes.ADAPTER_NODE) {
    return getPoliciesForAdapter(sourceNode)
  }

  if (sourceNode.type === NodeTypes.CLUSTER_NODE) {
    const group = sourceNode.data as Group
    const adapterIDs = group.childrenNodeIds.map<Node | undefined>((e) => workspaceNodes.find((x) => x.id === e))

    const policies: DataPolicy[] = []
    for (const node of adapterIDs) {
      if (node) {
        const hh = getPoliciesForAdapter(node)
        if (hh) policies.push(...hh)
      }
    }
    return Array.from(new Set(policies))
  }

  return undefined
}
