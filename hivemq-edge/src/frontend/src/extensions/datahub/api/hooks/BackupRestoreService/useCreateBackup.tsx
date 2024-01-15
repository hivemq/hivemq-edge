import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useCreateBackup = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: () => {
      return appClient.backupRestore.createBackup()
    },
  })
}
