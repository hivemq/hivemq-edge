import { useMutation } from '@tanstack/react-query'
import type { BehaviorPolicy } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useCreateBehaviorPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (requestBody: BehaviorPolicy) => {
      return appClient.dataHubBehaviorPolicies.createBehaviorPolicy(requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries([DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES])
    },
  })
}
