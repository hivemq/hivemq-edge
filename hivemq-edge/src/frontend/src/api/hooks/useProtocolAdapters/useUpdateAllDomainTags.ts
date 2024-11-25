import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type DomainTagList } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateAllDomainTagsProps {
  adapterId: string
  requestBody: DomainTagList
}

export const useUpdateAllDomainTags = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateAdapterDomainTags = ({ adapterId, requestBody }: UpdateAllDomainTagsProps) => {
    return appClient.protocolAdapters.updateAdapterDomainTags(adapterId, requestBody)
  }

  return useMutation<UpdateAllDomainTagsProps, ApiError, UpdateAllDomainTagsProps>({
    mutationFn: updateAdapterDomainTags,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, data.adapterId, QUERY_KEYS.DISCOVERY_TAGS] })
    },
  })
}
