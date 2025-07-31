import { http, HttpResponse } from 'msw'

import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import { MOCK_SIMPLE_SCHEMA_URI } from '@/__test-utils__/rjsf/schema.mocks.ts'
import type { ManagedAsset, ManagedAssetList, PulseActivationToken } from '@/api/__generated__'

export const MOCK_PULSE_ACTIVATION_TOKEN: PulseActivationToken = {
  token: MOCK_JWT,
}

export const MOCK_PULSE_ASSET: ManagedAsset = {
  id: '3b028f58-f949-4de1-9b8b-c1a35b1643a4',
  name: 'Test asset',
  description: 'The short description of the asset',
  topic: 'test/topic',
  schema: MOCK_SIMPLE_SCHEMA_URI,
}

export const MOCK_PULSE_ASSET_LIST: ManagedAssetList = {
  items: [MOCK_PULSE_ASSET],
}

export const handlers = [
  http.delete('**/api/v1/management/pulse/activation-token', () => {
    return HttpResponse.json('Token deleted', { status: 200 })
  }),

  http.post<never, PulseActivationToken>('**/api/v1/management/pulse/activation-token', async ({ request }) => {
    const data = await request.json()

    return HttpResponse.json({ created: data.token }, { status: 200 })
  }),

  http.get('**/management/pulse/managed-assets', () => {
    return HttpResponse.json<ManagedAssetList>(MOCK_PULSE_ASSET_LIST, { status: 200 })
  }),
]
