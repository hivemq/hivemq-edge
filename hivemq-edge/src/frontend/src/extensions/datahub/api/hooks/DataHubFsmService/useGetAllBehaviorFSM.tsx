import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError } from '@/api/__generated__'
import { type JsonNode } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useGetAllBehaviorFSM = () => {
  const appClient = useHttpClient()
  return useQuery<JsonNode, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.BEHAVIOR_FSM_MODEL],
    queryFn: () => appClient.dataHubFsm.getFsms(),
  })
}
