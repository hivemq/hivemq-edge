import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Adapter, ApiError } from '../../__generated__'

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
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.ADAPTERS])
    },
  })
}
