import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { RJSFSchema } from '@rjsf/utils'
import { useToast } from '@chakra-ui/react'

import type { DomainTag, DomainTagList } from '@/api/__generated__'
import { tagListJsonSchema, tagListUISchema } from '@/api/schemas/'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.ts'
import { useCreateDomainTags } from '@/api/hooks/useProtocolAdapters/useCreateDomainTags.ts'
import { useDeleteDomainTags } from '@/api/hooks/useProtocolAdapters/useDeleteDomainTags.ts'
import { useUpdateAllDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateAllDomainTags.ts'
import { useUpdateDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateDomainTags.ts'
import { useGetDomainTagSchema } from '@/api/hooks/useDomainModel/useGetDomainTagSchema.ts'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import type { ManagerContextType } from '@/modules/Mappings/types.ts'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'

export const useTagManager = (adapterId: string) => {
  const { t } = useTranslation()
  const toast = useToast(BASE_TOAST_OPTION)

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
    // TODO[28249] Handle manually until backend fixed
    const { properties } = rest

    const safeSchema = {
      ...rest,
      properties: {
        ...properties,
        protocolId: {
          const: protocol?.id,
          default: protocol?.id,
        },
      },
    }

    return {
      ...tagListJsonSchema,
      definitions: {
        TagSchema: safeSchema,
      },
    }
  }, [protocol?.id, tagSchema])

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
    toast.promise(
      deleteMutator.mutateAsync({ adapterId: adapterId, tagId: encodeURIComponent(tagId) }),
      formatToast('delete')
    )
  }

  const onCreate = (tag: DomainTag) => {
    toast.promise(createMutator.mutateAsync({ adapterId: adapterId, requestBody: tag }), formatToast('create'))
  }

  const onUpdate = (tagId: string, tag: DomainTag) => {
    toast.promise(
      updateMutator.mutateAsync({ adapterId: adapterId, tagId: encodeURIComponent(tagId), requestBody: tag }),
      formatToast('update')
    )
  }

  const onupdateCollection = (tags: DomainTagList) => {
    toast.promise(
      updateCollectionMutator.mutateAsync({ adapterId: adapterId, requestBody: tags }),
      formatToast('updateCollection')
    )
  }

  const context: ManagerContextType<DomainTagList> = {
    schema: tagListSchema,
    uiSchema: tagListUISchema,
    formData: tagList || { items: [] },
  }

  return {
    // The context of the operations
    context,
    capabilities: protocol?.capabilities,
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
