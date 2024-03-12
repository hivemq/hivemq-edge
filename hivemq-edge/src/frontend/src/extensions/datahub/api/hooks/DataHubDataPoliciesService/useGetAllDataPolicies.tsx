import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { ApiError, DataPolicyList } from '@/api/__generated__'

export const useGetAllDataPolicies = () => {
  const appClient = useHttpClient()
  return useQuery<DataPolicyList, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES],
    queryFn: async () => {
      return appClient.dataHubDataPolicies.getAllDataPolicies()
    },
  })
}
