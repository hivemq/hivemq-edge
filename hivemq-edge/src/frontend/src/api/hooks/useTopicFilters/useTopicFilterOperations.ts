import { useToast } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { TopicFilter } from '@/api/__generated__'

import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters.ts'
import { useCreateTopicFilter } from '@/api/hooks/useTopicFilters/useCreateTopicFilter.ts'
import { useDeleteTopicFilter } from '@/api/hooks/useTopicFilters/useDeleteTopicFilter.ts'
import { useUpdateTopicFilter } from '@/api/hooks/useTopicFilters/useUpdateTopicFilter.ts'

export const useTopicFilterOperations = () => {
  const { t } = useTranslation()
  const toast = useToast()

  const { data: topicFilterList, isLoading, isError, error } = useListTopicFilters()

  const createMutator = useCreateTopicFilter()
  const deleteMutator = useDeleteTopicFilter()
  const updateMutator = useUpdateTopicFilter()

  // TODO[NVL] Insert Edge-wide toast configuration (need refactoring)
  const formatToast = (operation: string) => ({
    success: {
      title: t(`device.drawer.tagList.toast.${operation}.title`),
      description: t(`topicFilter.toast.${operation}.description`, { context: 'success' }),
    },
    error: {
      title: t(`device.drawer.tagList.toast.${operation}.title`),
      description: t(`topicFilter.toast.${operation}.description`, { context: 'error' }),
    },
    loading: {
      title: t(`device.drawer.tagList.toast.${operation}.title`),
      description: t('topicFilter.toast.description', { context: 'loading' }),
    },
  })

  const onDelete = (topicName: string) => {
    toast.promise(deleteMutator.mutateAsync({ name: topicName }), formatToast('delete'))
  }

  const onCreate = (requestBody: TopicFilter) => {
    toast.promise(createMutator.mutateAsync({ requestBody: requestBody }), formatToast('create'))
  }

  const onUpdate = (filter: string, requestBody?: TopicFilter) => {
    toast.promise(updateMutator.mutateAsync({ name: filter, requestBody: requestBody }), formatToast('update'))
  }

  return {
    // The CRUD operations
    data: topicFilterList,
    onCreate,
    onDelete,
    onUpdate,
    // The state (as in ReactQuery)
    isLoading,
    isError,
    error,
    isPending: createMutator.isPending || updateMutator.isPending || deleteMutator.isPending, // assuming only one operation at a time
  }
}
