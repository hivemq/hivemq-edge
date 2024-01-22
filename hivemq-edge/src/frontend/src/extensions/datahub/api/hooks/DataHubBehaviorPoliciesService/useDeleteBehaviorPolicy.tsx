import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useDeleteBehaviorPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (policyId: string) => {
      return appClient.dataHubBehaviorPolicies.deleteBehaviorPolicy(policyId)
    },
  })
}
