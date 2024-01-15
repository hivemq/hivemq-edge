import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

interface InvalidateClientSessionProps {
  clientId: string
  preventWillMessage?: boolean
}

export const useDisconnectClient = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (data: InvalidateClientSessionProps) => {
      return appClient.mqttClients.disconnectClient(data.clientId, data.preventWillMessage || false)
    },
  })
}
