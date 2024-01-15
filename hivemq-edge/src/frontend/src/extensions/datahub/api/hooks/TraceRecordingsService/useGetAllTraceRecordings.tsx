import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetAllTraceRecordings = () => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.TRACE_RECORDING],
    queryFn: async () => {
      return appClient.traceRecordings.getAllTraceRecordings()
    },
  })
}
