import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'
import { type SouthboundMappingList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateSouthboundMappingsProps {
  adapterId: string
  requestBody: SouthboundMappingList
}

export const useUpdateSouthboundMappings = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateProtocolAdapter = ({ adapterId, requestBody }: UpdateSouthboundMappingsProps) => {
    return appClient.protocolAdapters.updateAdapterSouthboundMappings(adapterId, requestBody)
  }

  return useMutation<unknown, ApiError, UpdateSouthboundMappingsProps>({
    mutationFn: updateProtocolAdapter,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: [QUERY_KEYS.ADAPTERS, variables.adapterId, QUERY_KEYS.SOUTHBOUND_MAPPINGS],
      })
    },
  })
}
