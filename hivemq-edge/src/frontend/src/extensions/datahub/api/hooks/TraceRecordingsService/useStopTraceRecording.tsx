import { useMutation } from '@tanstack/react-query'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'
import type { TraceRecordingItem } from '@/extensions/datahub/api/__generated__'

interface StopTraceRecordingProps {
  traceRecordingId: string
  requestBody?: TraceRecordingItem
}

export const useStopTraceRecording = () => {
  const appClient = useHttpClient()

  return useMutation({
    mutationFn: (data: StopTraceRecordingProps) => {
      return appClient.traceRecordings.stopTraceRecording(data.traceRecordingId, data.requestBody)
    },
  })
}
