import { useQuery } from '@tanstack/react-query'
import type { ApiError, JsonNode } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

/**
 * The direction of the tag schema to fetch.
 * - SOUTHBOUND (write) describes the shape a write targets; the non-writable envelope is omitted. Whether an
 *   individual field can be written is carried per field as readOnly.
 * - Omitted (the default) fetches the NORTHBOUND (read) schema: the full data shape published for the tag
 *   (tagName, timestamp, value, metadata). The parameter is then left off the request entirely, so the URL is
 *   unchanged for read callers.
 */
export type SchemaDirection = 'NORTHBOUND' | 'SOUTHBOUND'

export const useGetSchema = (adapterId: string, tagName: string, direction?: SchemaDirection) => {
  const appClient = useHttpClient()

  return useQuery<JsonNode, ApiError>({
    // The direction is part of the key: the two directions are different schemas for the same tag. The
    // northbound (default) key deliberately has no direction slot so it stays identical to the key used by
    // useGetCombinedDataSchemas for the same document — one cache entry, one fetch.
    queryKey: direction
      ? [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS, tagName, direction]
      : [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS, tagName],
    queryFn: () => appClient.protocolAdapters.getSchema(adapterId, encodeURIComponent(tagName), direction),
  })
}
