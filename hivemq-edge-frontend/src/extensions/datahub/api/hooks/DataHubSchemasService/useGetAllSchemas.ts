import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { PolicySchema } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'
import { usePaginatedList } from '@datahub/api/hooks/usePaginatedList.ts'

export const useGetAllSchemas = () => {
  const appClient = useHttpClient()
  // The listing is cursor-paginated; fetch every page so items beyond the first page
  // (default size 50) are not hidden from the UI. See EDG-844.
  return usePaginatedList<PolicySchema>([DATAHUB_QUERY_KEYS.SCHEMAS], (cursor) =>
    appClient.dataHubSchemas.getAllSchemas(undefined, undefined, undefined, undefined, cursor)
  )
}
