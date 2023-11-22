import { rest } from 'msw'
import { GitHubReleases } from '../types.ts'

export const handlers = [
  rest.get('https://api.github.com/repos/hivemq/hivemq-edge/releases', (_, res, ctx) => {
    return res(ctx.json<GitHubReleases[]>([]), ctx.status(200))
  }),
]
