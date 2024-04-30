import { useQuery } from '@tanstack/react-query'
import { ApiError, CapabilityList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

import { MOCK_CAPABILITIES } from './__handlers__'

const getLocalConfig = (): CapabilityList | undefined => {
  const config: string | undefined =
    import.meta.env.MODE === 'development' ? import.meta.env.VITE_FLAG_CAPABILITIES : undefined
  if (!config) return undefined

  console.log('%c[HiveMQ Edge] Capability override: %s', 'color:#ffc000;font-weight:bold;', config)

  const capabilities = config.split(',')
  return { items: MOCK_CAPABILITIES.items?.filter((e) => capabilities.includes(e.id as string)) }
}

export const useGetCapabilities = () => {
  const appClient = useHttpClient()

  return useQuery<CapabilityList, ApiError>({
    queryKey: [QUERY_KEYS.FRONTEND_CAPABILITIES],
    queryFn: async () => {
      const item = await appClient.frontend.getCapabilities()
      return getLocalConfig() || item
    },
  })
}
