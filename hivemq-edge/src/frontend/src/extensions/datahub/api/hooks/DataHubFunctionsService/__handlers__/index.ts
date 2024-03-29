import { rest } from 'msw'
import { type JsonNode } from '@/api/__generated__'

import mockFunctions from '@datahub/api/__generated__/schemas/_functions.json'

export const handlers = [
  rest.get('*/data-hub/functions', (_, res, ctx) => {
    return res(ctx.json<JsonNode>(mockFunctions), ctx.status(200))
  }),
]
