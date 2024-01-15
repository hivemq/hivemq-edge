import { useMutation } from '@tanstack/react-query'
import type { DataPolicy } from '../../__generated__'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

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
  })
}
