import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import { MOCK_SIMPLE_SCHEMA_URI } from '@/__test-utils__/rjsf/schema.mocks.ts'
import type { ManagedAsset, ManagedAssetList, PulseActivationToken } from '@/api/__generated__'
import { AssetMapping, DataIdentifierReference } from '@/api/__generated__'
import { delay, http, HttpResponse } from 'msw'
import status = AssetMapping.status
import type = DataIdentifierReference.type

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

export const MOCK_PULSE_ASSET_MAPPED: ManagedAsset = {
  id: '3b028f58-f949-4de1-9b8b-c1a35b1643a5',
  name: 'Test mapped asset',
  description: 'The short description of the mapped asset',
  topic: 'test/topic/2',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    status: status.STREAMING,
    sources: [
      { id: 'test', type: type.TOPIC_FILTER },
      { id: 'test/2', type: type.TAG },
      { id: 'test/3', type: type.PULSE_ASSET },
    ],
    primary: { id: 'test', type: type.TOPIC_FILTER },
    instructions: [],
  },
}

export const MOCK_PULSE_ASSET_MAPPED_XX: ManagedAsset = {
  id: '3b028f58-f949-4de1-9b8b-c1a35b1643a9',
  name: 'Test other asset',
  description: 'The short name of the mapped asset',
  topic: 'test/topic/2',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    status: status.REQUIRES_REMAPPING,
    sources: [
      { id: 'test/2', type: type.TAG },
      { id: 'test/3', type: type.PULSE_ASSET },
    ],
    primary: { id: 'test/2', type: type.TAG },
    instructions: [],
  },
}

export const MOCK_PULSE_ASSET_LIST: ManagedAssetList = {
  items: [MOCK_PULSE_ASSET, MOCK_PULSE_ASSET_MAPPED, MOCK_PULSE_ASSET_MAPPED_XX],
}

export const handlers = [
  http.delete('**/api/v1/management/pulse/activation-token', async () => {
    await delay(1000)

    return HttpResponse.json('Token deleted', { status: 200 })
  }),

  http.post<never, PulseActivationToken>('**/api/v1/management/pulse/activation-token', async ({ request }) => {
    const data = await request.json()
    await delay(1000)

    return HttpResponse.json({ created: data.token }, { status: 200 })
  }),

  http.get('**/management/pulse/managed-assets', () => {
    return HttpResponse.json<ManagedAssetList>(MOCK_PULSE_ASSET_LIST, { status: 200 })
  }),

  http.post<never, ManagedAsset>('**/management/pulse/managed-assets', async ({ request }) => {
    const data = await request.json()
    await delay(1000)

    return HttpResponse.json({ created: data.id }, { status: 200 })
  }),

  http.delete<never>('**/management/pulse/managed-assets/:assetId', async ({ params }) => {
    const { assetId } = params
    await delay(1000)

    return HttpResponse.json({ deleted: assetId }, { status: 200 })
  }),

  http.put<never, ManagedAsset>('**/management/pulse/managed-assets/:assetId', async ({ params, request }) => {
    const { assetId } = params
    const data = await request.json()
    await delay(1000)

    return HttpResponse.json({ updated: assetId, data: data.name }, { status: 200 })
  }),
]
