import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import queryClient from '@/api/queryClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useDeleteSchema = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (schemaId: string) => {
      return appClient.dataHubSchemas.deleteSchema(schemaId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries([DATAHUB_QUERY_KEYS.SCHEMAS])
    },
  })
}
