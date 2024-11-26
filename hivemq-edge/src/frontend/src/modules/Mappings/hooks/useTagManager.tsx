import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { RJSFSchema } from '@rjsf/utils'
import { useToast } from '@chakra-ui/react'

import { DomainTag, DomainTagList } from '@/api/__generated__'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
import { useCreateDomainTags } from '@/api/hooks/useProtocolAdapters/useCreateDomainTags.ts'
import { useDeleteDomainTags } from '@/api/hooks/useProtocolAdapters/useDeleteDomainTags.ts'
import { useUpdateAllDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateAllDomainTags.ts'
import { useUpdateDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateDomainTags.ts'
import { useGetDomainTagSchema } from '@/api/hooks/useDomainModel/useGetDomainTagSchema.ts'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import { ManagerContextType } from '@/modules/Mappings/types.ts'

export const useTagManager = (adapterId: string) => {
  const { t } = useTranslation()
  const toast = useToast()

  const { protocol, isLoading: protocolLoad } = useGetAdapterInfo(adapterId)
  const { data: tagSchema, isError: isSchemaError, error: errorSchema } = useGetDomainTagSchema(protocol?.id)
  const { data: tagList, isLoading, isError: isTagError, error: errorTag } = useGetDomainTags(adapterId)

  const createMutator = useCreateDomainTags()
  const deleteMutator = useDeleteDomainTags()
  const updateMutator = useUpdateDomainTags()
  const updateCollectionMutator = useUpdateAllDomainTags()

  const tagListSchema = useMemo<RJSFSchema | undefined>(() => {
    if (!tagSchema) return undefined

    const { $schema: sc, ...rest } = tagSchema?.configSchema as RJSFSchema
    return {
      // $schema: 'https://json-schema.org/draft/2020-12/schema',
      definitions: {
        TagSchema: rest,
      },
      properties: {
        items: {
          type: 'array',
          title: 'List of tags',
          description: 'The list of all tags defined in the device',
          items: {
            $ref: '#/definitions/TagSchema',
          },
        },
      },
    }
  }, [tagSchema])

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

  const onDelete = (tagId: string) => {
    if (!adapterId) return
    toast.promise(
      deleteMutator.mutateAsync({ adapterId: adapterId, tagId: encodeURIComponent(tagId) }),
      formatToast('delete')
    )
  }

  const onCreate = (tag: DomainTag) => {
    if (!adapterId) return
    toast.promise(createMutator.mutateAsync({ adapterId: adapterId, requestBody: tag }), formatToast('create'))
  }

  const onUpdate = (tagId: string, tag: DomainTag) => {
    if (!adapterId) return
    toast.promise(
      updateMutator.mutateAsync({ adapterId: adapterId, tagId: encodeURIComponent(tagId), requestBody: tag }),
      formatToast('update')
    )
  }

  const onupdateCollection = (tags: DomainTagList) => {
    if (!adapterId) return
    toast.promise(
      updateCollectionMutator.mutateAsync({ adapterId: adapterId, requestBody: tags }),
      formatToast('updateCollection')
    )
  }

  const context: ManagerContextType = {
    schema: tagListSchema,
    uiSchema: {
      'ui:submitButtonOptions': {
        norender: true,
      },

      items: {
        items: {
          'ui:order': ['name', 'description', '*'],
          'ui:collapsable': {
            titleKey: 'name',
          },
        },
      },
    },
    formData: tagList || { items: [] },
  }

  return {
    // The context of the operations
    context,
    // The CRUD operations
    data: tagList || { items: [] },
    onCreate,
    onDelete,
    onUpdate,
    onupdateCollection,
    // The state (as in ReactQuery)
    isLoading: isLoading || protocolLoad,
    isError: isTagError || isSchemaError,
    error: errorTag?.message || errorSchema?.message,
    isPending:
      createMutator.isPending ||
      updateMutator.isPending ||
      deleteMutator.isPending ||
      updateCollectionMutator.isPending, // assuming only one operation at a time
  }
}
