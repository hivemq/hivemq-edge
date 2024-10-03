import { useMemo } from 'react'
import { type JSONSchema7 } from 'json-schema'
import { type RJSFSchema, type UiSchema } from '@rjsf/utils'

import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { type MappingManagerType } from '@/modules/Mappings/types.ts'
import { MOCK_MAPPING_DATA, MOCK_OUTWARD_SUBSCRIPTION_OPCUA } from '@/modules/Mappings/utils/subscription.utils.ts'
import { getMainRootFromPath, getTopicPaths } from '@/modules/Workspace/utils/topics-utils.ts'
import { getInwardMappingRootProperty, isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'

export const useMappingManager = (adapterId: string) => {
  const { data: allProtocols, isLoading: isProtocolLoading } = useGetAdapterTypes()
  const { data: allAdapters, isLoading: isAdapterLoading } = useListProtocolAdapters()

  const adapterInfo = useMemo(() => {
    const selectedAdapter = allAdapters?.find((adapter) => adapter.id === adapterId)
    if (!selectedAdapter) return undefined

    const selectedProtocol = allProtocols?.items?.find((protocol) => protocol.id === selectedAdapter.type)
    if (!selectedProtocol) return undefined

    return { selectedAdapter, selectedProtocol }
  }, [allAdapters, allProtocols?.items, adapterId])

  const inwardManager = useMemo<MappingManagerType | undefined>(() => {
    if (!adapterInfo) return undefined
    const { selectedProtocol, selectedAdapter } = adapterInfo

    const { properties } = selectedProtocol?.configSchema as JSONSchema7
    if (!properties) return undefined

    // TODO[NVL] This is still a hack; backend needs to provide identification of subscription properties
    const paths = getTopicPaths(selectedProtocol?.configSchema || {})
    const mappingIndex = getMainRootFromPath(paths)
    if (!mappingIndex) return undefined

    const formData = selectedAdapter.config?.[mappingIndex]
    if (!formData) return undefined

    const mappingProperties = properties?.[mappingIndex]
    if (!mappingProperties) return undefined

    const mappingPropName = getInwardMappingRootProperty(selectedProtocol.id as string)
    const schema: RJSFSchema = {
      $schema: 'https://json-schema.org/draft/2020-12/schema',
      type: 'object',
      properties: {
        [mappingPropName]: mappingProperties,
      },
    }
    const { ['ui:tabs']: tabs, ...rest } = selectedProtocol.uiSchema as UiSchema
    return { schema, formData: { [mappingPropName]: formData }, uiSchema: rest }
  }, [adapterInfo])

  const outwardManager = useMemo<MappingManagerType | undefined>(() => {
    if (!adapterInfo) return undefined
    const { selectedProtocol } = adapterInfo

    if (!isBidirectional(selectedProtocol)) return undefined

    return {
      schema: MOCK_OUTWARD_SUBSCRIPTION_OPCUA.schema || {},
      formData: {
        subscriptions: MOCK_MAPPING_DATA,
      },
      uiSchema: MOCK_OUTWARD_SUBSCRIPTION_OPCUA.uiSchema || {},
    }
  }, [adapterInfo])

  const isLoading = isAdapterLoading || isProtocolLoading

  return { isLoading, inwardManager, outwardManager }
}
