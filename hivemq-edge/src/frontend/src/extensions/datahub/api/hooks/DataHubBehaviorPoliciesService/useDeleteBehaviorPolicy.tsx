import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useDeleteBehaviorPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (policyId: string) => {
      return appClient.dataHubBehaviorPolicies.deleteBehaviorPolicy(policyId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries([DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES])
    },
  })
}
