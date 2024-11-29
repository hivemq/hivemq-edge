import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type FieldMappingsListModel } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateDomainTagsProps {
  adapterId: string
  requestBody: FieldMappingsListModel
}

export const useUpdateAdapterFieldMappings = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateAdapterDomainTags = ({ adapterId, requestBody }: UpdateDomainTagsProps) => {
    return appClient.protocolAdapters.updateAdapterFieldMappings(adapterId, requestBody)
  }

  return useMutation<UpdateDomainTagsProps, ApiError, UpdateDomainTagsProps>({
    mutationFn: updateAdapterDomainTags,
    onSuccess: (data) => {
      queryClient.invalidateQueries({
        queryKey: [QUERY_KEYS.ADAPTERS, data.adapterId, QUERY_KEYS.DISCOVERY_FIELD_MAPPINGS],
      })
    },
  })
}
