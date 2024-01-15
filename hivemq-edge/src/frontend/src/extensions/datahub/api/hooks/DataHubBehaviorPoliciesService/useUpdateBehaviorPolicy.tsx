import { useMutation } from '@tanstack/react-query'
import type { BehaviorPolicy } from '../../__generated__'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

interface UpdateDataPolicyProps {
  policyId: string
  requestBody: BehaviorPolicy
}

export const useUpdateBehaviorPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (data: UpdateDataPolicyProps) => {
      return appClient.dataHubBehaviorPolicies.updateBehaviorPolicy(data.policyId, data.requestBody)
    },
  })
}
