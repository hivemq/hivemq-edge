import { useQuery } from '@tanstack/react-query'
import { useSimpleHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import type { ApiError, AuthMode } from '@/api/__generated__'

/**
 * Reports which authentication mode the gateway is configured for (local username/password or OIDC),
 * so the login page can present the matching UI. Unauthenticated call, fired on the login page.
 */
export const useGetAuthMode = () => {
  const appClient = useSimpleHttpClient()

  return useQuery<AuthMode, ApiError>({
    queryKey: [QUERY_KEYS.AUTHENTICATION_MODE],
    queryFn: () => appClient.authentication.authMode(),
  })
}
