import axios from 'axios'
import { useQuery } from '@tanstack/react-query'

import { ApiError } from '@/api/__generated__'
import { GitHubReleases } from '@/api/hooks/useGitHub/types.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetReleases = () => {
  return useQuery<GitHubReleases[], ApiError>([QUERY_KEYS.GITHUB_RELEASES], async () => {
    const retrievePosts = async () => {
      const response = await axios.get<GitHubReleases[]>('https://api.github.com/repos/hivemq/hivemq-edge/releases')
      return response.data
    }

    return await retrievePosts()
  })
}
