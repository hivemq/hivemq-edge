import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '../../__generated__'
import { type DomainTag } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateDomainTagsProps {
  adapterId: string
  requestBody: DomainTag
}

export const useCreateDomainTags = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createAdapterDomainTags = ({ adapterId, requestBody }: CreateDomainTagsProps) => {
    return appClient.protocolAdapters.addAdapterDomainTags(adapterId, requestBody)
  }

  return useMutation<unknown, ApiError, CreateDomainTagsProps>({
    mutationFn: createAdapterDomainTags,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, variables.adapterId, QUERY_KEYS.DISCOVERY_TAGS] })
    },
  })
}
