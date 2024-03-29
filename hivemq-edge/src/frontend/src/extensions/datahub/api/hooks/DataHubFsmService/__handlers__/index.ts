import { rest } from 'msw'
import { type JsonNode } from '@/api/__generated__'

import model from '@datahub/api/__generated__/schemas/BehaviorPolicyData.json'

export const handlers = [
  rest.get('*/data-hub/fsm', (_, res, ctx) => {
    return res(ctx.json<JsonNode>(model), ctx.status(200))
  }),
]
