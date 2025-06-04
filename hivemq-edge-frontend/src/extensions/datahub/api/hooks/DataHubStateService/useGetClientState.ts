import { useQuery } from '@tanstack/react-query'

import type { ApiError, FsmStatesInformationListItem } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useGetClientState = (clientId: string) => {
  const appClient = useHttpClient()
  return useQuery<FsmStatesInformationListItem, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.CLIENT_STATE, clientId],
    queryFn: () => appClient.dataHubState.getClientState(clientId),
  })
}
