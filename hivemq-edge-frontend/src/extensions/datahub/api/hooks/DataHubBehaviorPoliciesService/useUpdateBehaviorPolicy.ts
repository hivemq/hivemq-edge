import { useMutation } from '@tanstack/react-query'
import type { ApiError, BehaviorPolicy } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

interface UpdateDataPolicyProps {
  policyId: string
  requestBody: BehaviorPolicy
}

export const useUpdateBehaviorPolicy = () => {
  const appClient = useHttpClient()

  return useMutation<BehaviorPolicy, ApiError, UpdateDataPolicyProps>({
    mutationFn: (data: UpdateDataPolicyProps) => {
      return appClient.dataHubBehaviorPolicies.updateBehaviorPolicy(data.policyId, data.requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES] })
    },
  })
}
