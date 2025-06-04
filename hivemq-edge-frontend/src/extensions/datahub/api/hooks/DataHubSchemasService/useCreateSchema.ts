import { useMutation } from '@tanstack/react-query'
import type { ApiError, PolicySchema } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useCreateSchema = () => {
  const appClient = useHttpClient()

  return useMutation<PolicySchema, ApiError, PolicySchema>({
    mutationFn: (requestBody: PolicySchema) => {
      return appClient.dataHubSchemas.createSchema(requestBody)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS] })
    },
  })
}
