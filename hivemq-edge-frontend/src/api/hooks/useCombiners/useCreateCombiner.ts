import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError, Combiner } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateCombinerProps {
  requestBody: Combiner
}

export const useCreateCombiner = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createCombiner = ({ requestBody }: CreateCombinerProps) => {
    return appClient.combiners.addCombiner(requestBody)
  }

  return useMutation<unknown, ApiError, CreateCombinerProps>({
    mutationFn: createCombiner,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.COMBINER] })
    },
  })
}
