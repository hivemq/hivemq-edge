import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useDeleteDataPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (policyId: string) => {
      return appClient.dataHubDataPolicies.deleteDataPolicy(policyId)
    },
  })
}
