import { useMemo } from 'react'
import { type JSONSchema7 } from 'json-schema'
import { type RJSFSchema, type UiSchema } from '@rjsf/utils'

import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { type SubscriptionManagerType } from '@/modules/Subscriptions/types.ts'
import { MOCK_OUTWARD_SUBSCRIPTION_OPCUA } from '@/modules/Subscriptions/utils/subscription.utils.ts'
import { getTopicPaths } from '@/modules/Workspace/utils/topics-utils.ts'

export const useSubscriptionManager = (adapterId: string) => {
  const { data: allProtocols, isLoading: isProtocolLoading } = useGetAdapterTypes()
  const { data: allAdapters, isLoading: isAdapterLoading } = useListProtocolAdapters()

  const adapterInfo = useMemo(() => {
    const selectedAdapter = allAdapters?.find((adapter) => adapter.id === adapterId)
    if (!selectedAdapter) return undefined

    const selectedProtocol = allProtocols?.items?.find((protocol) => protocol.id === selectedAdapter.type)
    if (!selectedProtocol) return undefined

    return { selectedAdapter, selectedProtocol }
  }, [allAdapters, allProtocols?.items, adapterId])

  const inwardManager = useMemo<SubscriptionManagerType | undefined>(() => {
    if (!adapterInfo) return undefined
    const { selectedProtocol, selectedAdapter } = adapterInfo

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
  }, [adapterInfo])

  const outwardManager = useMemo<SubscriptionManagerType | undefined>(() => {
    if (!adapterInfo) return undefined
    const { selectedProtocol } = adapterInfo

    if (!['opc-ua-client'].includes(selectedProtocol.id || '')) return undefined

    return {
      schema: MOCK_OUTWARD_SUBSCRIPTION_OPCUA.schema || {},
      formData: { subscriptions: [] },
      uiSchema: MOCK_OUTWARD_SUBSCRIPTION_OPCUA.uiSchema || {},
    }
  }, [adapterInfo])

  const isLoading = isAdapterLoading || isProtocolLoading

  return { isLoading, inwardManager, outwardManager }
}
