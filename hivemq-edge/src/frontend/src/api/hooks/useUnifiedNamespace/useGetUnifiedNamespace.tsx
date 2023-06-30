import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { ApiError, ISA95ApiBean } from '@/api/__generated__'

export const useGetUnifiedNamespace = () => {
  const appClient = useHttpClient()

  return useQuery<ISA95ApiBean, ApiError>([QUERY_KEYS.UNIFIED_NAMESPACE], async () => {
    const item = await appClient.uns.getIsa95()
    return item
  })
}
