import { http, HttpResponse } from 'msw'
import type { JsonNode } from '@/api/__generated__'

import model from '@datahub/api/__generated__/schemas/BehaviorPolicyData.json'

export const handlers = [
  http.get('*/data-hub/fsm', () => {
    return HttpResponse.json<JsonNode>(model, { status: 200 })
  }),
]
