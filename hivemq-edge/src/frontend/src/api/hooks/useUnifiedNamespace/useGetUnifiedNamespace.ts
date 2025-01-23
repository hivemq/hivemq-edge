import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import type { ApiError, ISA95ApiBean } from '@/api/__generated__'

export const useGetUnifiedNamespace = () => {
  const appClient = useHttpClient()

  return useQuery<ISA95ApiBean, ApiError>({
    queryKey: [QUERY_KEYS.UNIFIED_NAMESPACE],
    queryFn: () => appClient.uns.getIsa95(),
  })
}
