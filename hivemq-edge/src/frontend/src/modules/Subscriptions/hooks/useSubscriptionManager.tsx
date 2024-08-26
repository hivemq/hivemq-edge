import { useMemo } from 'react'
import { type Node, useNodes } from 'reactflow'
import { type JSONSchema7 } from 'json-schema'
import { type RJSFSchema, type UiSchema } from '@rjsf/utils'

import { type Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { type SubscriptionManagerType } from '@/modules/Subscriptions/types.ts'
import { MOCK_OUTWARD_SUBSCRIPTION_OPCUA } from '@/modules/Subscriptions/utils/subscription.utils.ts'
import { getTopicPaths } from '@/modules/Workspace/utils/topics-utils.ts'

export const useSubscriptionManager = (id: string) => {
  const nodes = useNodes()
  const { data: allProtocols } = useGetAdapterTypes()
  const { data: allAdapters } = useListProtocolAdapters()

  const device = useMemo(() => {
    const selectedNode = nodes.find((node) => node.id === id && node.type === NodeTypes.ADAPTER_NODE) as
      | Node<Adapter>
      | undefined
    if (!selectedNode) return undefined
    const selectedProtocol = allProtocols?.items?.find((protocol) => protocol.id === selectedNode?.data.type)
    if (!selectedProtocol) return undefined
    const selectedAdapter = allAdapters?.find((adapter) => adapter.id === selectedNode?.data.id)
    if (!selectedAdapter) return undefined

    return { selectedNode, selectedAdapter, selectedProtocol }
  }, [allAdapters, allProtocols?.items, id, nodes])

  const inwardManager = useMemo<SubscriptionManagerType | undefined>(() => {
    if (!device) return undefined
    const { selectedProtocol, selectedAdapter } = device

    const { properties } = selectedProtocol?.configSchema as JSONSchema7
    if (!properties) return undefined

    // TODO[NVL] This is still a hack; backend needs to provide identification of subscription properties
    const paths = getTopicPaths(selectedProtocol?.configSchema || {})
    const subIndex = paths.shift()?.split('.').shift()
    if (!subIndex) return undefined

    const formData = selectedAdapter.config?.[subIndex]
    if (!formData) return undefined

    const subs = properties?.[subIndex]
    if (!subs) return undefined

    const schema: RJSFSchema = {
      $schema: 'https://json-schema.org/draft/2020-12/schema',
      type: 'object',
      properties: {
        subscriptions: subs,
      },
      required: ['subscriptions'],
    }
    const { ['ui:tabs']: tabs, ...rest } = selectedProtocol.uiSchema as UiSchema
    return { schema, formData: { subscriptions: formData }, uiSchema: rest }
  }, [device])

  const outwardManager = useMemo<SubscriptionManagerType | undefined>(() => {
    if (!device) return undefined
    const { selectedProtocol } = device

    if (!['opc-ua-client'].includes(selectedProtocol.id || '')) return undefined

    return {
      schema: MOCK_OUTWARD_SUBSCRIPTION_OPCUA.schema || {},
      formData: { subscriptions: [] },
      uiSchema: MOCK_OUTWARD_SUBSCRIPTION_OPCUA.uiSchema || {},
    }
  }, [device])

  return { inwardManager, outwardManager }
}
