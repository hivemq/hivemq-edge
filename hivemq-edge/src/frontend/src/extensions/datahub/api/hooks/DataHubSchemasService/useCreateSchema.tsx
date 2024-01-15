import { useMutation } from '@tanstack/react-query'
import type { Schema } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useCreateSchema = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (requestBody: Schema) => {
      return appClient.dataHubSchemas.createSchema(requestBody)
    },
  })
}
