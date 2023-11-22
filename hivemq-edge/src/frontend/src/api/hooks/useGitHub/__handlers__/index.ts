import { rest } from 'msw'
import { GitHubReleases } from '../types.ts'

export const MOCK_GITHUB_RELEASE: GitHubReleases = {
  name: '2023.XXX',
  html_url: 'http://localhost.com',
}
export const handlers = [
  rest.get('https://api.github.com/repos/hivemq/hivemq-edge/releases', (_, res, ctx) => {
    return res(ctx.json<GitHubReleases[]>([MOCK_GITHUB_RELEASE]), ctx.status(200))
  }),
]
