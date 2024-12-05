import { useMutation } from '@tanstack/react-query'
import { ApiError } from '@/api/__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useStartSamplingForTopic = () => {
  const appClient = useHttpClient()

  const createAdapterDomainTags = (topic: string) => {
    return appClient.payloadSampling.startSamplingForTopic(encodeURIComponent(topic))
  }

  return useMutation<string, ApiError, string>({
    mutationFn: createAdapterDomainTags,
  })
}
