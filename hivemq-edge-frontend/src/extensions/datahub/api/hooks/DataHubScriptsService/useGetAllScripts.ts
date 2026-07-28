import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { Script } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'
import { usePaginatedList } from '@datahub/api/hooks/usePaginatedList.ts'

interface GetAllScriptsProps {
  fields?: string
  functionTypes?: string
  scriptIds?: string
}

export const useGetAllScripts = ({ fields, functionTypes, scriptIds }: GetAllScriptsProps) => {
  const appClient = useHttpClient()
  // The listing is cursor-paginated; fetch every page so items beyond the first page
  // (default size 50) are not hidden from the UI. See EDG-844.
  return usePaginatedList<Script>([DATAHUB_QUERY_KEYS.SCRIPTS, fields, functionTypes, scriptIds], (cursor) =>
    appClient.dataHubScripts.getAllScripts(fields, functionTypes, scriptIds, 500, cursor)
  )
}
