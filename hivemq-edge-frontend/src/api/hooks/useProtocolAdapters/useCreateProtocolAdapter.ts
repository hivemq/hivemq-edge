import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { Adapter, ApiError, StatusList } from '../../__generated__'
import { Status } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateProtocolAdapterProps {
  adapterType: string
  requestBody: Adapter
}

export const useCreateProtocolAdapter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createProtocolAdapter = ({ adapterType, requestBody }: CreateProtocolAdapterProps) => {
    return appClient.protocolAdapters.addAdapter(adapterType, requestBody)
  }

  return useMutation<unknown, ApiError, CreateProtocolAdapterProps>({
    mutationFn: createProtocolAdapter,
    onMutate: (createProtocolAdapter) => {
      queryClient.setQueryData<StatusList>([QUERY_KEYS.ADAPTERS, QUERY_KEYS.CONNECTION_STATUS], (old) => {
        const optimisticUpdate: Status = {
          connection: Status.connection.DISCONNECTED,
          id: createProtocolAdapter.requestBody.id,
          type: createProtocolAdapter.requestBody.type,
        }
        return {
          items: [...(old?.items || []), optimisticUpdate],
        }
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS] })
    },
  })
}
