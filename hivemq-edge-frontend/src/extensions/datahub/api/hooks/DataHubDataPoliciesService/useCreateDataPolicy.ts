import { useMutation } from '@tanstack/react-query'
import type { ApiError, DataPolicy } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useCreateDataPolicy = () => {
  const appClient = useHttpClient()

  return useMutation<DataPolicy, ApiError, DataPolicy>({
    mutationFn: (requestBody: DataPolicy) => {
      return appClient.dataHubDataPolicies.createDataPolicy(requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES] })
    },
  })
}
