import { useMutation } from '@tanstack/react-query'
import type { DataPolicy } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useCreateDataPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (requestBody: DataPolicy) => {
      return appClient.dataHubDataPolicies.createDataPolicy(requestBody)
    },
  })
}
