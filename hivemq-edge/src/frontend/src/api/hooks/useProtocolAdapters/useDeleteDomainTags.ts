import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface DeleteDomainTagsProps {
  adapterId: string
  tagId: string
}

export const useDeleteDomainTags = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteAdapterDomainTags = ({ adapterId, tagId }: DeleteDomainTagsProps) => {
    return appClient.protocolAdapters.deleteAdapterDomainTags(adapterId, tagId)
  }

  return useMutation<DeleteDomainTagsProps, ApiError, DeleteDomainTagsProps>({
    mutationFn: deleteAdapterDomainTags,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, data.adapterId, QUERY_KEYS.DISCOVERY_TAGS] })
    },
  })
}
