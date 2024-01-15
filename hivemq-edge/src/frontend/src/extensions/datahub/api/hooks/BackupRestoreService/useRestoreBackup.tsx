import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useRestoreBackup = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (backupId: string) => {
      return appClient.backupRestore.restoreBackup(backupId)
    },
  })
}
