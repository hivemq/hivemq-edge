import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'

import { type NorthboundMappingList } from '@/api/__generated__'
import { useListNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useListNorthboundMappings.ts'
import { useUpdateNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useUpdateNorthboundMappings.ts'

import { ManagerContextType, MappingManagerType } from '@/modules/Mappings/types.ts'
import { northboundMappingListSchema } from '@/api/schemas/northbound.json-schema.ts'
import { northboundMappingListUISchema } from '@/api/schemas/northbound.ui-schema.ts'

export const useNorthboundMappingManager = (adapterId: string): MappingManagerType<NorthboundMappingList> => {
  const { t } = useTranslation()
  const toast = useToast()

  const { data, isError, isLoading, error } = useListNorthboundMappings(adapterId)

  const updateCollectionMutator = useUpdateNorthboundMappings()

  // TODO[NVL] Insert Edge-wide toast configuration (need refactoring)
  const formatToast = (operation: string) => ({
    success: {
      title: t(`device.drawer.tagList.toast.${operation}.title`),
      description: t(`device.drawer.tagList.toast.${operation}.description`, { context: 'success' }),
    },
    error: {
      title: t(`device.drawer.tagList.toast.${operation}.title`),
      description: t(`device.drawer.tagList.toast.${operation}.description`, { context: 'error' }),
    },
    loading: {
      title: t(`device.drawer.tagList.toast.${operation}.title`),
      description: t('device.drawer.tagList.toast.description', { context: 'loading' }),
    },
  })

  const onUpdateCollection = (tags: NorthboundMappingList) => {
    if (!adapterId) return
    toast.promise(
      updateCollectionMutator.mutateAsync({ adapterId: adapterId, requestBody: tags }),
      formatToast('updateCollection')
    )
  }

  const context: ManagerContextType = {
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
    // The state (as in ReactQuery)
    isLoading: isLoading,
    isError: isError,
    error: error,
    isPending: updateCollectionMutator.isPending,
  }
}
