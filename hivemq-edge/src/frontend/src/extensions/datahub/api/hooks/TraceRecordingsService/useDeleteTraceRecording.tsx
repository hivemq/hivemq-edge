import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useDeleteTraceRecording = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (traceRecordingId: string) => {
      return appClient.traceRecordings.deleteTraceRecording(traceRecordingId)
    },
  })
}
