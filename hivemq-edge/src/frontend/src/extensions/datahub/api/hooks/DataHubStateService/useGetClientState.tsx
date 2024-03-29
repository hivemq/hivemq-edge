import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { ApiError, type FsmStatesInformationListItem } from '@/api/__generated__'

export const useGetClientState = (clientId: string) => {
  const appClient = useHttpClient()
  return useQuery<FsmStatesInformationListItem, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.CLIENT_STATE, clientId],
    queryFn: async () => {
      return appClient.dataHubState.getClientState(clientId)
    },
  })
}
