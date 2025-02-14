import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError, Combiner } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateCombinerProps {
  combinerId: string
  requestBody: Combiner
}

export const useUpdateCombiner = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateCombiner = ({ combinerId, requestBody }: UpdateCombinerProps) => {
    return appClient.combiners.updateCombiner(combinerId, requestBody)
  }

  return useMutation<unknown, ApiError, UpdateCombinerProps>({
    mutationFn: updateCombiner,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.COMBINER] })
    },
  })
}
