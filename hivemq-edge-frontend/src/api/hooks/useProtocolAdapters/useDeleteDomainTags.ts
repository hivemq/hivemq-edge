import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '../../__generated__'

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

  return useMutation<unknown, ApiError, DeleteDomainTagsProps>({
    mutationFn: deleteAdapterDomainTags,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, variables.adapterId, QUERY_KEYS.DISCOVERY_TAGS] })
    },
  })
}
