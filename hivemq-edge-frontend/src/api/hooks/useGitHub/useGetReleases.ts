import axios from 'axios'
import { useQuery } from '@tanstack/react-query'

import type { ApiError } from '@/api/__generated__'
import type { GitHubReleases } from '@/api/hooks/useGitHub/types.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetReleases = () => {
  return useQuery<GitHubReleases[], ApiError>({
    queryKey: [QUERY_KEYS.GITHUB_RELEASES],
    queryFn: async () => {
      const retrievePosts = async () => {
        const response = await axios.get<GitHubReleases[]>('https://api.github.com/repos/hivemq/hivemq-edge/releases')
        return response.data
      }

      return await retrievePosts()
    },
  })
}
