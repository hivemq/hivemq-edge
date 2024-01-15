import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useDeleteScript = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (scriptId: string) => {
      return appClient.dataHubScripts.deleteScript(scriptId)
    },
  })
}
