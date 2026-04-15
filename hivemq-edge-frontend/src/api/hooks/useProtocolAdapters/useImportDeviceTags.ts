import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'
import { axiosInstance } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import config from '@/config'
import type { BrowseFormat } from './useBrowseDeviceTags.ts'

export type ImportMode = 'CREATE' | 'DELETE' | 'OVERWRITE' | 'MERGE_SAFE' | 'MERGE_OVERWRITE'

interface ImportDeviceTagsProps {
  adapterId: string
  file: File
  format: BrowseFormat
  mode?: ImportMode
}

export const useImportDeviceTags = () => {
  const { credentials } = useAuth()
  const queryClient = useQueryClient()

  return useMutation<unknown, ApiError, ImportDeviceTagsProps>({
    mutationFn: async ({ adapterId, file, format, mode = 'MERGE_SAFE' }) => {
      const params = new URLSearchParams({ mode })
      const url = `${config.apiBaseUrl}/api/v1/management/protocol-adapters/adapters/${adapterId}/device-tags/import?${params}`
      const body = await file.arrayBuffer()
      return axiosInstance.post(url, body, {
        headers: {
          'Content-Type': format,
          Authorization: `Bearer ${credentials?.token}`,
        },
      })
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ADAPTERS, variables.adapterId, QUERY_KEYS.DISCOVERY_TAGS] })
    },
  })
}
