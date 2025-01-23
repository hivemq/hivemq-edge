import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'

import { type NorthboundMappingList } from '@/api/__generated__'
import { useListNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useListNorthboundMappings.ts'
import { useUpdateNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useUpdateNorthboundMappings.ts'

import { northboundMappingListSchema } from '@/api/schemas/northbound.json-schema.ts'
import { northboundMappingListUISchema } from '@/api/schemas/northbound.ui-schema.ts'
import { DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'
import type { ManagerContextType, MappingManagerType } from '@/modules/Mappings/types.ts'

export const useNorthboundMappingManager = (adapterId: string): MappingManagerType<NorthboundMappingList> => {
  const { t } = useTranslation()
  const toast = useToast({
    duration: DEFAULT_TOAST_OPTION.duration,
    isClosable: DEFAULT_TOAST_OPTION.isClosable,
  })
  const { data, isError, isLoading, error } = useListNorthboundMappings(adapterId)

  const updateCollectionMutator = useUpdateNorthboundMappings()

  const formatToast = (operation: string) => ({
    success: {
      title: t(`protocolAdapter.mapping.toast.${operation}.title`),
      description: t(`protocolAdapter.mapping.toast.${operation}.description`, { context: 'success' }),
    },
    error: {
      title: t(`protocolAdapter.mapping.toast.${operation}.title`),
      description: t(`protocolAdapter.mapping.toast.${operation}.description`, { context: 'error' }),
    },
    loading: {
      title: t(`protocolAdapter.mapping.toast.${operation}.title`),
      description: t('protocolAdapter.mapping.toast.description', { context: 'loading' }),
    },
  })

  const onUpdateCollection = (tags: NorthboundMappingList) => {
    if (!adapterId) return undefined
    const promise = updateCollectionMutator.mutateAsync({ adapterId: adapterId, requestBody: tags })
    toast.promise(promise, formatToast('updateCollection'))
    return promise
  }

  const context: ManagerContextType<NorthboundMappingList> = {
    schema: northboundMappingListSchema,
    uiSchema: northboundMappingListUISchema,
    formData: data,
  }

  return {
    // The context of the operations
    context,
    // The CRUD operations
    data: data,
    onUpdateCollection,
    onClose: () => toast.closeAll(),
    // The state (as in ReactQuery)
    isLoading: isLoading,
    isError: isError,
    error: error,
    isPending: updateCollectionMutator.isPending,
  }
}
