import { useQuery } from '@tanstack/react-query'
import type { ApiError, JsonNode } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

/**
 * The direction of the tag schema to fetch.
 * - WRITE describes only what can be written to the tag; the non-writable envelope is omitted.
 * - Omitted (the default) describes the full data shape published for the tag (tagName, timestamp, value,
 *   metadata). The parameter is then left off the request entirely, so the URL is unchanged for read callers.
 */
export type SchemaDirection = 'READ' | 'WRITE'

export const useGetSchema = (adapterId: string, tagName: string, direction?: SchemaDirection) => {
  const appClient = useHttpClient()

  return useQuery<JsonNode, ApiError>({
    // The direction is part of the key: READ and WRITE are different schemas for the same tag.
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS, tagName, direction],
    queryFn: () => appClient.protocolAdapters.getSchema(adapterId, encodeURIComponent(tagName), direction),
  })
}
