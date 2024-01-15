import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useCreateDiagnosticArchive = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: () => {
      return appClient.diagnosticArchive.createDiagnosticArchive()
    },
  })
}
