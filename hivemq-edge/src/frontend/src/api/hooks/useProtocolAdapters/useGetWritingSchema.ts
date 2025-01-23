import { useQuery } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'
import { type TagSchema } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetWritingSchema = (adapterId: string, tagName: string) => {
  const appClient = useHttpClient()

  return useQuery<TagSchema, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS, tagName],
    queryFn: () => appClient.protocolAdapters.getWritingSchema(adapterId, encodeURIComponent(tagName)),
  })
}
