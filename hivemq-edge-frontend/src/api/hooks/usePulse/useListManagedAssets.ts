import { useQuery } from '@tanstack/react-query'

import type { ApiError, ManagedAssetList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListManagedAssets = () => {
  const appClient = useHttpClient()

  return useQuery<ManagedAssetList, ApiError>({
    queryKey: [QUERY_KEYS.PULSE_ASSETS],
    queryFn: () => appClient.pulse.getManagedAssets(),
    enabled: hasPulse !== undefined,
  })
}
