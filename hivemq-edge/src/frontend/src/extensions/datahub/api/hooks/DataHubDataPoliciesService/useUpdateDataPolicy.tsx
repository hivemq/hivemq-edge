import { useMutation } from '@tanstack/react-query'
import type { DataPolicy } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

interface UpdateDataPolicyProps {
  policyId: string
  requestBody: DataPolicy
}

export const useUpdateDataPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (data: UpdateDataPolicyProps) => {
      return appClient.dataHubDataPolicies.updateDataPolicy(data.policyId, data.requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries([DATAHUB_QUERY_KEYS.DATA_POLICIES])
    },
  })
}
