import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import type { DataPolicy } from '@/api/__generated__'
import { usePaginatedList } from '@datahub/api/hooks/usePaginatedList.ts'

export const useGetAllDataPolicies = () => {
  const appClient = useHttpClient()
  // The listing is cursor-paginated; fetch every page so items beyond the first page
  // (default size 50) are not hidden from the UI. See EDG-844.
  return usePaginatedList<DataPolicy>([DATAHUB_QUERY_KEYS.DATA_POLICIES], (cursor) =>
    appClient.dataHubDataPolicies.getAllDataPolicies(undefined, undefined, undefined, undefined, undefined, cursor)
  )
}
