import { useMemo } from 'react'
import type { RJSFSchema } from '@rjsf/utils'
import { useToast } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { $TopicFilter, type TopicFilter, type TopicFilterList } from '@/api/__generated__'

import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters.ts'
import { useCreateTopicFilter } from '@/api/hooks/useTopicFilters/useCreateTopicFilter.ts'
import { useDeleteTopicFilter } from '@/api/hooks/useTopicFilters/useDeleteTopicFilter.ts'
import { useUpdateTopicFilter } from '@/api/hooks/useTopicFilters/useUpdateTopicFilter.ts'
import { useUpdateAllTopicFilter } from '@/api/hooks/useTopicFilters/useUpdateAllTopicFilters.ts'
import { ManagerContextType } from '@/modules/Mappings/types.ts'

interface TopicFilterSchemas {
  schema: RJSFSchema
}

export const useTopicFilterOperations = () => {
  const { t } = useTranslation()
  const toast = useToast()

  const { data: topicFilterList, isLoading, isError, error } = useListTopicFilters()

  const createMutator = useCreateTopicFilter()
  const deleteMutator = useDeleteTopicFilter()
  const updateMutator = useUpdateTopicFilter()
  const updateCollectionMutator = useUpdateAllTopicFilter()

  // TODO[24980] This is due to limitation of the openapi-typescript-codegen library
  //  - schemas are not properly exported as reusable JSONSchema
  //  - the split in different files doesn't allow programmatic manipulation
  //  - some information (required, format ) is missing (partly due to limited quality of generated openAPI
  const topicFilterSchemas = useMemo<TopicFilterSchemas>(() => {
    return {
      schema: {
        // $schema: 'https://json-schema.org/draft/2020-12/schema',
        definitions: {
          TopicFilter: $TopicFilter,
        },
        properties: {
          items: {
            type: 'array',
            // title: 'List of tags',
            // description: 'The list of all tags defined in the device',
            items: {
              $ref: '#/definitions/TopicFilter',
            },
          },
        },
      },
    }
  }, [])

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

  const onUpdateCollection = (requestBody: TopicFilterList) => {
    toast.promise(updateCollectionMutator.mutateAsync({ requestBody: requestBody }), formatToast('updateCollection'))
  }

  const context: ManagerContextType = {
    schema: topicFilterSchemas.schema,
    uiSchema: {},
    formData: topicFilterList,
  }

  return {
    // The schema context
    context,
    // The CRUD operations
    data: topicFilterList,
    onCreate,
    onDelete,
    onUpdate,
    onUpdateCollection,
    // The state (as in ReactQuery)
    isLoading,
    isError,
    error,
    isPending:
      createMutator.isPending ||
      updateMutator.isPending ||
      deleteMutator.isPending ||
      updateCollectionMutator.isPending, // assuming only one operation at a time
  }
}
