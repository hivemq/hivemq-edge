import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetAllBackups = () => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.BACKUP_RESTORE],
    queryFn: async () => {
      return appClient.backupRestore.getAllBackups()
    },
  })
}
