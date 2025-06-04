import { useQuery } from '@tanstack/react-query'
import type { ApiError, JsonNode } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetWritingSchema = (adapterId: string, tagName: string) => {
  const appClient = useHttpClient()

  return useQuery<JsonNode, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS, tagName],
    queryFn: () => appClient.protocolAdapters.getWritingSchema(adapterId, encodeURIComponent(tagName)),
  })
}
