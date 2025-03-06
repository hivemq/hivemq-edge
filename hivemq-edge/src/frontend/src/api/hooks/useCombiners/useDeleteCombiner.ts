import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '../../__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface DeleteCombinerProps {
  combinerId: string
}

export const useDeleteCombiner = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteCombiner = ({ combinerId }: DeleteCombinerProps) => {
    return appClient.combiners.deleteCombiner(combinerId)
  }

  return useMutation<unknown, ApiError, DeleteCombinerProps>({
    mutationFn: deleteCombiner,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.COMBINER] })
    },
  })
}
