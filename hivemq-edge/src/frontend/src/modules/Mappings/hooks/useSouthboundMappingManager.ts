import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'

import { type SouthboundMappingList } from '@/api/__generated__'
import { useListSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useListSouthboundMappings.ts'
import { useUpdateSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useUpdateSouthboundMappings.ts'

import { ManagerContextType, MappingManagerType } from '@/modules/Mappings/types.ts'
import { southboundMappingListSchema } from '@/api/schemas/southbound.json-schema.ts'
import { southboundMappingListUISchema } from '@/api/schemas/southbound.ui-schema.ts'

export const useSouthboundMappingManager = (adapterId: string): MappingManagerType<SouthboundMappingList> => {
  const { t } = useTranslation()
  const toast = useToast()

  const { data, isError, isLoading, error } = useListSouthboundMappings(adapterId)

  const updateCollectionMutator = useUpdateSouthboundMappings()

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

  const onUpdateCollection = (tags: SouthboundMappingList) => {
    if (!adapterId) return
    toast.promise(
      updateCollectionMutator.mutateAsync({ adapterId: adapterId, requestBody: tags }),
      formatToast('updateCollection')
    )
  }

  const context: ManagerContextType = {
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
    // The state (as in ReactQuery)
    isLoading: isLoading,
    isError: isError,
    error: error,
    isPending: updateCollectionMutator.isPending,
  }
}
