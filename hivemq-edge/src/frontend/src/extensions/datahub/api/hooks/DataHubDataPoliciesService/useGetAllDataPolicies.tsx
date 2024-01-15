import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetAllDataPolicies = () => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES],
    queryFn: async () => {
      return appClient.dataHubDataPolicies.getAllDataPolicies()
    },
  })
}
