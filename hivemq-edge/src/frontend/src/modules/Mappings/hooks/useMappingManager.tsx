import { useCallback, useMemo } from 'react'
import { type RJSFSchema, type UiSchema } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'

import { MOCK_MAPPING_DATA, MOCK_OUTWARD_MAPPING_OPCUA } from '@/__test-utils__/adapters/mapping.utils.ts'
import { ApiError, DomainTagList } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useUpdateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useUpdateProtocolAdapter.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'
import { type MappingManagerType } from '@/modules/Mappings/types.ts'
import { getMainRootFromPath, getTopicPaths } from '@/modules/Workspace/utils/topics-utils.ts'
import { getInwardMappingRootProperty, isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'

export const useMappingManager = (adapterId: string) => {
  const { t } = useTranslation()
  const { data: allProtocols, isLoading: isProtocolLoading } = useGetAdapterTypes()
  const { data: allAdapters, isLoading: isAdapterLoading } = useListProtocolAdapters()
  const updateProtocolAdapter = useUpdateProtocolAdapter()
  const { successToast, errorToast } = useEdgeToast()

  const processMutation = useCallback(
    (promise: Promise<unknown>) => {
      promise
        .then(() => {
          successToast({
            title: t('protocolAdapter.toast.update.title'),
            description: t('protocolAdapter.toast.update.description'),
          })
        })
        .catch((err: ApiError) =>
          errorToast(
            {
              title: t('protocolAdapter.toast.update.title'),
              description: t('protocolAdapter.toast.update.error'),
            },
            err
          )
        )
    },
    [errorToast, successToast, t]
  )

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

    const { properties } = selectedProtocol?.configSchema as RJSFSchema
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
      type: 'object',
      properties: {
        [mappingPropName]: mappingProperties,
      },
    }
    const { ['ui:tabs']: tabs, ...rest } = selectedProtocol.uiSchema as UiSchema
    return {
      schema,
      formData: { [mappingPropName]: formData },
      uiSchema: {
        ...rest,
        'ui:submitButtonOptions': {
          norender: true,
        },
      },
      onSubmit: (data) =>
        processMutation(
          updateProtocolAdapter.mutateAsync({
            adapterId: adapterId,
            requestBody: {
              id: adapterId,
              type: adapterInfo.selectedProtocol.id,
              config: { ...selectedAdapter.config, ...data },
            },
          })
        ),
    }
  }, [adapterId, adapterInfo, processMutation, updateProtocolAdapter])

  const outwardManager = useMemo<MappingManagerType | undefined>(() => {
    if (!adapterInfo) return undefined
    const { selectedProtocol } = adapterInfo

    if (!isBidirectional(selectedProtocol)) return undefined

    return {
      schema: MOCK_OUTWARD_MAPPING_OPCUA.schema || {},
      formData: {
        subscriptions: MOCK_MAPPING_DATA,
      },
      uiSchema: MOCK_OUTWARD_MAPPING_OPCUA.uiSchema || {},
    }
  }, [adapterInfo])

  const isLoading = isAdapterLoading || isProtocolLoading

  return { isLoading, inwardManager, outwardManager }
}
