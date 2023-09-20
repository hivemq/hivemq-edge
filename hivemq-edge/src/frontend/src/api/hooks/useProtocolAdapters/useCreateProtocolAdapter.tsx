import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Adapter, ApiError, StatusList, Status } from '../../__generated__'

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

  return useMutation<unknown, ApiError, CreateProtocolAdapterProps>(createProtocolAdapter, {
    onMutate: (a) => {
      queryClient.setQueryData<StatusList>([QUERY_KEYS.ADAPTERS, QUERY_KEYS.CONNECTION_STATUS], (old) => {
        const optimisticUpdate: Status = {
          connection: Status.connection.DISCONNECTED,
          id: a.requestBody.id,
          type: a.requestBody.type,
        }
        return {
          items: [...(old?.items || []), optimisticUpdate],
        }
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.ADAPTERS])
    },
  })
}
