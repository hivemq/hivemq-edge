import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface DeleteTopicFilterProps {
  name: string
}

export const useDeleteTopicFilter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteTopicFilter = ({ name }: DeleteTopicFilterProps) => {
    return appClient.topicFilters.deleteTopicFilter(encodeURIComponent(name))
  }

  return useMutation<DeleteTopicFilterProps, ApiError, DeleteTopicFilterProps>({
    mutationFn: deleteTopicFilter,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS] })
    },
  })
}
