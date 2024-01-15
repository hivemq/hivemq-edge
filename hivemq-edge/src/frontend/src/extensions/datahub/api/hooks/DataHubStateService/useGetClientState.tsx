import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetClientState = (clientId: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.CLIENT_STATE, clientId],
    queryFn: async () => {
      return appClient.dataHubState.getClientState(clientId)
    },
  })
}
