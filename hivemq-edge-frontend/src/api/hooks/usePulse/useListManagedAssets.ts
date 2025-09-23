import { useQuery } from '@tanstack/react-query'

import type { ApiError, ManagedAssetList } from '@/api/__generated__'
import { Capability } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListManagedAssets = () => {
  const appClient = useHttpClient()
  const { data: hasPulse, isLoading: isCapacityLoading } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  const query = useQuery<ManagedAssetList, ApiError>({
    queryKey: [QUERY_KEYS.PULSE_ASSETS],
    queryFn: () => appClient.pulse.getManagedAssets(),
    enabled: hasPulse !== undefined,
  })

  return {
    // eslint-disable-next-line @tanstack/query/no-rest-destructuring
    ...query,
    isLoading: isCapacityLoading || query.isLoading,
  }
}
