import { http, HttpResponse } from 'msw'
import type { JsonNode } from '@/api/__generated__'

import mockFunctions from '@datahub/api/__generated__/schemas/_functions.json'

export const handlers = [
  http.get('*/data-hub/behavior-validation/states/:clientId', () => {
    return HttpResponse.json<JsonNode>(mockFunctions, { status: 200 })
  }),
]
