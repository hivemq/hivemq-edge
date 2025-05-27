import { useQuery } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'
import { type TagSchema } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetDomainTagSchema = (protocolId: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery<TagSchema, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TAGS, protocolId],
    queryFn: () => appClient.protocolAdapters.getTagSchema(protocolId as string),
    enabled: Boolean(protocolId),
  })
}
