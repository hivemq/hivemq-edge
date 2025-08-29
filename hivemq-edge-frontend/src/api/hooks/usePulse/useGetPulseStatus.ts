import { useQuery } from '@tanstack/react-query'

import config from '@/config'

import type { ApiError, PulseStatus } from '@/api/__generated__'
import { Capability } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetPulseStatus = () => {
  const appClient = useHttpClient()
  const { data: hasPulse } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  return useQuery<PulseStatus, ApiError>({
    queryKey: [QUERY_KEYS.PULSE_STATUS],
    queryFn: () => appClient.pulse.getPulseStatus(),

    enabled: hasPulse !== undefined,
    retry: 0,
    refetchInterval: config.httpClient.pollingRefetchInterval,
  })
}
