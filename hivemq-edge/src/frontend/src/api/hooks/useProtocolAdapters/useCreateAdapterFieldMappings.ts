import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type FieldMappingsModel } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateDomainTagsProps {
  adapterId: string
  requestBody: FieldMappingsModel
}

export const useCreateAdapterFieldMappings = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createAdapterDomainTags = ({ adapterId, requestBody }: CreateDomainTagsProps) => {
    return appClient.protocolAdapters.addAdapterFieldMappings(adapterId, requestBody)
  }

  return useMutation<CreateDomainTagsProps, ApiError, CreateDomainTagsProps>({
    mutationFn: createAdapterDomainTags,
    onSuccess: (data) => {
      queryClient.invalidateQueries({
        queryKey: [QUERY_KEYS.ADAPTERS, data.adapterId, QUERY_KEYS.DISCOVERY_FIELD_MAPPINGS],
      })
    },
  })
}
