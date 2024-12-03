import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, type NorthboundMappingList } from '../../__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateNorthboundMappingsProps {
  adapterId: string
  requestBody: NorthboundMappingList
}

export const useUpdateNorthboundMappings = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateProtocolAdapter = ({ adapterId, requestBody }: UpdateNorthboundMappingsProps) => {
    return appClient.protocolAdapters.updateAdapterNorthboundMappings(adapterId, requestBody)
  }

  return useMutation<UpdateNorthboundMappingsProps, ApiError, UpdateNorthboundMappingsProps>({
    mutationFn: updateProtocolAdapter,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, data.adapterId, QUERY_KEYS.NORTHBOUND_MAPPINGS] })
    },
  })
}
