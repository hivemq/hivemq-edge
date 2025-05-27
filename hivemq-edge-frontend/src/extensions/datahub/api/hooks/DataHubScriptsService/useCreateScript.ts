import { useMutation } from '@tanstack/react-query'
import type { ApiError, Script } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useCreateScript = () => {
  const appClient = useHttpClient()

  return useMutation<Script, ApiError, Script>({
    mutationFn: (requestBody: Script) => {
      return appClient.dataHubScripts.createScript(requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.SCRIPTS] })
    },
  })
}
