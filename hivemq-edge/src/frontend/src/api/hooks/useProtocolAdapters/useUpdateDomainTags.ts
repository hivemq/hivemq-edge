import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type DomainTag } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateDomainTagsProps {
  adapterId: string
  tagId: string
  requestBody: DomainTag
}

export const useUpdateDomainTags = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateAdapterDomainTags = ({ adapterId, tagId, requestBody }: UpdateDomainTagsProps) => {
    return appClient.protocolAdapters.updateAdapterDomainTag(adapterId, tagId, requestBody)
  }

  return useMutation<unknown, ApiError, UpdateDomainTagsProps>({
    mutationFn: updateAdapterDomainTags,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, variables.adapterId, QUERY_KEYS.DISCOVERY_TAGS] })
    },
  })
}
