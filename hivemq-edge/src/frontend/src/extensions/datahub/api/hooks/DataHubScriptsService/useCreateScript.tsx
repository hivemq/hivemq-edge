import { useMutation } from '@tanstack/react-query'
import type { Script } from '@/api/__generated__'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useCreateScript = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (requestBody: Script) => {
      return appClient.dataHubScripts.createScript(requestBody)
    },
  })
}
