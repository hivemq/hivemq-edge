import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetMqttClientSubscriptions = (clientId: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.MQTT_CLIENTS, clientId, 'subscriptions'],
    queryFn: async () => {
      return appClient.mqttClients.getSubscriptionsForMqttClient(clientId)
    },
  })
}
