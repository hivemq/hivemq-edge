import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError, ISA95ApiBean } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateUnifiedNamespaceProps {
  requestBody: ISA95ApiBean
}

export const useSetUnifiedNamespace = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateUnifiedNamespace = ({ requestBody }: UpdateUnifiedNamespaceProps) => {
    return appClient.uns.setIsa95(requestBody)
  }

  return useMutation<unknown, ApiError, UpdateUnifiedNamespaceProps>({
    mutationFn: updateUnifiedNamespace,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.UNIFIED_NAMESPACE] })
    },
  })
}
