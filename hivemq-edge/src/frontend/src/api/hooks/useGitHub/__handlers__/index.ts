import { http, HttpResponse } from 'msw'
import type { GitHubReleases } from '../types.ts'

export const MOCK_GITHUB_RELEASE: GitHubReleases = {
  name: '2023.XXX',
  html_url: 'http://localhost.com',
}
export const handlers = [
  http.get('https://api.github.com/repos/hivemq/hivemq-edge/releases', () => {
    return HttpResponse.json<GitHubReleases[]>([MOCK_GITHUB_RELEASE], { status: 200 })
  }),
]
