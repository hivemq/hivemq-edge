import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useDownloadBackupFile = (backupId: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.BACKUP_RESTORE, backupId, 'download'],
    queryFn: async () => {
      return appClient.backupRestore.downloadBackupFile(backupId)
    },
  })
}
