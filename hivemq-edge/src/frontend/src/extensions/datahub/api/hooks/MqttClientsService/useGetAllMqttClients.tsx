import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

interface GetAllMqttClientsProps {
  limit?: number
  cursor?: string
}

export const useGetAllMqttClients = ({ limit, cursor }: GetAllMqttClientsProps) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.MQTT_CLIENTS],
    queryFn: async () => {
      return appClient.mqttClients.getAllMqttClients(limit, cursor)
    },
  })
}
