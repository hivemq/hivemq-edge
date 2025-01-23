import { useMutation } from '@tanstack/react-query'
import type { ApiError, Schema } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useCreateSchema = () => {
  const appClient = useHttpClient()

  return useMutation<Schema, ApiError, Schema>({
    mutationFn: (requestBody: Schema) => {
      return appClient.dataHubSchemas.createSchema(requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS] })
    },
  })
}
