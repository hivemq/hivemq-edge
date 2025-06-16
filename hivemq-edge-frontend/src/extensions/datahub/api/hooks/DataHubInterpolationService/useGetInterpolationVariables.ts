import { useQuery } from '@tanstack/react-query'

import type { ApiError, InterpolationVariableList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useGetInterpolationVariables = () => {
  const appClient = useHttpClient()
  return useQuery<InterpolationVariableList, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.INTERPOLATION_VARIABLES],
    queryFn: () => appClient.dataHubInterpolation.getVariables(),
  })
}
