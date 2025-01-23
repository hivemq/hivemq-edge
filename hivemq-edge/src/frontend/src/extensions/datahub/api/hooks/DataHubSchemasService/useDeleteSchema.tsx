import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'
import type { ApiError } from '@/api/__generated__'

export const useDeleteSchema = () => {
  const appClient = useHttpClient()

  return useMutation<void, ApiError, string>({
    mutationFn: (schemaId: string) => {
      return appClient.dataHubSchemas.deleteSchema(schemaId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS] })
    },
  })
}
