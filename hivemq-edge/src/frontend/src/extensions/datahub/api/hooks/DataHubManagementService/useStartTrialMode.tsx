import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useStartTrialMode = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: () => {
      return appClient.dataHubManagement.startTrialMode()
    },
  })
}
