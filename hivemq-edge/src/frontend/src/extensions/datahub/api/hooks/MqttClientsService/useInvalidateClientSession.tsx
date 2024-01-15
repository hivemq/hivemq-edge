import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

interface InvalidateClientSessionProps {
  clientId: string
  preventWillMessage?: boolean
}

export const useInvalidateClientSession = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (data: InvalidateClientSessionProps) => {
      return appClient.mqttClients.invalidateClientSession(data.clientId, data.preventWillMessage || false)
    },
  })
}
