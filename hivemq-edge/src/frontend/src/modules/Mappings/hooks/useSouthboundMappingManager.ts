import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'

import { type SouthboundMappingList } from '@/api/__generated__'
import { useListSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useListSouthboundMappings.ts'
import { useUpdateSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useUpdateSouthboundMappings.ts'

import { ManagerContextType, MappingManagerType } from '@/modules/Mappings/types.ts'
import { southboundMappingListSchema } from '@/api/schemas/southbound.json-schema.ts'
import { southboundMappingListUISchema } from '@/api/schemas/southbound.ui-schema.ts'
import { DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'

export const useSouthboundMappingManager = (adapterId: string): MappingManagerType<SouthboundMappingList> => {
  const { t } = useTranslation()
  const toast = useToast({
    duration: DEFAULT_TOAST_OPTION.duration,
    isClosable: DEFAULT_TOAST_OPTION.isClosable,
  })

  const { data, isError, isLoading, error } = useListSouthboundMappings(adapterId)

  const updateCollectionMutator = useUpdateSouthboundMappings()

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

  const onUpdateCollection = (tags: SouthboundMappingList) => {
    if (!adapterId) return
    const promise = updateCollectionMutator.mutateAsync({ adapterId: adapterId, requestBody: tags })
    toast.promise(promise, formatToast('updateCollection'))
    return promise
  }

  const context: ManagerContextType<SouthboundMappingList> = {
    schema: southboundMappingListSchema,
    uiSchema: southboundMappingListUISchema,
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
