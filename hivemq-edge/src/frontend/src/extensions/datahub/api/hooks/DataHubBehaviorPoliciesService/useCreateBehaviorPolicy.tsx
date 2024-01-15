import { useMutation } from '@tanstack/react-query'
import type { BehaviorPolicy } from '../../__generated__'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useCreateBehaviorPolicy = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (requestBody: BehaviorPolicy) => {
      return appClient.dataHubBehaviorPolicies.createBehaviorPolicy(requestBody)
    },
  })
}
