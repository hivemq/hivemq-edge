import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'
import type { TraceRecordingItem } from '@/extensions/datahub/api/__generated__'

export const useCreateTraceRecording = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (requestBody?: TraceRecordingItem) => {
      return appClient.traceRecordings.createTraceRecording(requestBody)
    },
  })
}
