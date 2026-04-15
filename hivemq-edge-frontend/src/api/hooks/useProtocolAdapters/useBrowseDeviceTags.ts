import { useMutation } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'
import { axiosInstance } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'
import config from '@/config'

export type BrowseFormat = 'text/csv' | 'application/json' | 'application/yaml'

interface BrowseDeviceTagsProps {
  adapterId: string
  format?: BrowseFormat
  rootId?: string
  maxDepth?: number
}

export const useBrowseDeviceTags = () => {
  const { credentials } = useAuth()

  return useMutation<Blob, ApiError, BrowseDeviceTagsProps>({
    mutationFn: async ({ adapterId, format = 'text/csv', rootId, maxDepth }) => {
      const params = new URLSearchParams()
      if (rootId) params.set('rootId', rootId)
      if (maxDepth !== undefined) params.set('maxDepth', String(maxDepth))
      const query = params.toString()
      const url = `${config.apiBaseUrl}/api/v1/management/protocol-adapters/adapters/${adapterId}/device-tags/browse${query ? `?${query}` : ''}`
      const response = await axiosInstance.post<Blob>(url, null, {
        headers: {
          Accept: format,
          Authorization: `Bearer ${credentials?.token}`,
        },
        responseType: 'blob',
      })
      return response.data
    },
  })
}
