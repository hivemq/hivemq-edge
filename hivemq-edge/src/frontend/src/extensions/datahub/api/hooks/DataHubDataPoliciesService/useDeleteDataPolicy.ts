import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import type { ApiError } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useDeleteDataPolicy = () => {
  const appClient = useHttpClient()

  return useMutation<void, ApiError, string>({
    mutationFn: (policyId: string) => {
      return appClient.dataHubDataPolicies.deleteDataPolicy(policyId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES] })
    },
  })
}
