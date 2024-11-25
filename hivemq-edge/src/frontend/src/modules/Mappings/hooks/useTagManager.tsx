import { useMemo } from 'react'
import type { RJSFSchema } from '@rjsf/utils'
import { useToast } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { DomainTag, DomainTagList } from '@/api/__generated__'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
import { useCreateDomainTags } from '@/api/hooks/useProtocolAdapters/useCreateDomainTags.ts'
import { useDeleteDomainTags } from '@/api/hooks/useProtocolAdapters/useDeleteDomainTags.ts'
import { useUpdateAllDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateAllDomainTags.ts'
import { useUpdateDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateDomainTags.ts'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import { getInwardMappingSchema } from '@/modules/Workspace/utils/adapter.utils.ts'
import { createSchema } from '@/modules/Device/utils/tags.utils.ts'
import { ManagerContextType } from '@/modules/Mappings/types.ts'

interface TagManagerSchema {
  isError?: boolean
  error?: string
  data?: RJSFSchema
}

export const useTagManager = (adapterId: string) => {
  const { t } = useTranslation()
  const toast = useToast()

  const { protocol, isLoading: protocolLoad } = useGetAdapterInfo(adapterId)
  const tagManager = useMemo<TagManagerSchema>(() => {
    try {
      if (!protocol) return { isError: true, error: t('device.errors.noAdapter') }

      const schema = getInwardMappingSchema(protocol)
      const tagSchema = createSchema(schema.items as RJSFSchema)
      return { data: tagSchema }
    } catch (e) {
      return { isError: true, error: (e as Error).message }
    }
  }, [protocol, t])
  const { data: tagList, isLoading, isError: isTagError, error: tagError } = useGetDomainTags(adapterId, protocol?.id)

  // TODO[NVL] Error formats differ too much. ProblemDetails!
  const { error, isError } = useMemo(() => {
    if (!adapterId) return { error: t('device.errors.noAdapter'), isError: true }
    if (tagManager.isError) return { error: tagManager.error, isError: true }
    return { error: tagError, isError: isTagError }
  }, [adapterId, isTagError, t, tagError, tagManager.error, tagManager.isError])

  const createMutator = useCreateDomainTags()
  const deleteMutator = useDeleteDomainTags()
  const updateMutator = useUpdateDomainTags()
  const updateCollectionMutator = useUpdateAllDomainTags()

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
    schema: tagManager.data,
    uiSchema: {},
    formData: tagList,
  }

  return {
    // The context of the operations
    context,
    // The CRUD operations
    data: tagList,
    onCreate,
    onDelete,
    onUpdate,
    onupdateCollection,
    // The state (as in ReactQuery)
    isLoading: isLoading || protocolLoad,
    isError,
    error,
    isPending:
      createMutator.isPending ||
      updateMutator.isPending ||
      deleteMutator.isPending ||
      updateCollectionMutator.isPending, // assuming only one operation at a time
  }
}
