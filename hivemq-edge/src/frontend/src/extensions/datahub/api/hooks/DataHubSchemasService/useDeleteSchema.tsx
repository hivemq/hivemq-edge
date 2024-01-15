import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useDeleteSchema = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (schemaId: string) => {
      return appClient.dataHubSchemas.deleteSchema(schemaId)
    },
  })
}
