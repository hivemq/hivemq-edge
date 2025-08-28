import { delay, http, HttpResponse } from 'msw'

import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import { MOCK_SIMPLE_SCHEMA_URI } from '@/__test-utils__/rjsf/schema.mocks.ts'
import type { ManagedAsset, ManagedAssetList, PulseActivationToken } from '@/api/__generated__'
import { AssetMapping, PulseStatus } from '@/api/__generated__'

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
    mappingId: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
    status: AssetMapping.status.STREAMING,
  },
}

export const MOCK_PULSE_ASSET_MAPPED_DUPLICATE: ManagedAsset = {
  id: '3b028f58-f949-4de1-9b8b-c1a35b1643a9',
  name: 'Test other asset',
  description: 'The short name of the mapped asset',
  topic: 'test/topic/2',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    status: AssetMapping.status.REQUIRES_REMAPPING,
    mappingId: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
  },
}

export const MOCK_PULSE_ASSET_MAPPED_UNIQUE: ManagedAsset = {
  id: '3b028f58-f949-4de1-9b8b-c1a35b1643a7',
  name: 'Almost the same asset',
  description: 'Not sure how to describe that re-mapped asset',
  topic: 'test/topic/4',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    status: AssetMapping.status.REQUIRES_REMAPPING,
    mappingId: '3009a8c5-dba5-40a5-857c-2d0a9cd71637',
  },
}

export const MOCK_PULSE_ASSET_LIST: ManagedAssetList = {
  items: [MOCK_PULSE_ASSET, MOCK_PULSE_ASSET_MAPPED, MOCK_PULSE_ASSET_MAPPED_DUPLICATE, MOCK_PULSE_ASSET_MAPPED_UNIQUE],
}

export const MOCK_PULSE_STATUS_CONNECTED: PulseStatus = {
  activation: PulseStatus.activation.ACTIVATED,
  runtime: PulseStatus.runtime.CONNECTED,
}

export const MOCK_PULSE_STATUS_ERROR: PulseStatus = {
  activation: PulseStatus.activation.ACTIVATED,
  runtime: PulseStatus.runtime.ERROR,
  message: {
    title: 'Cannot connect to Pulse',
  },
}

export const MOCK_PULSE_STATUS_DISCONNECTED: PulseStatus = {
  activation: PulseStatus.activation.ACTIVATED,
  runtime: PulseStatus.runtime.DISCONNECTED,
}

export const MOCK_PULSE_STATUS_DEACTIVATED: PulseStatus = {
  activation: PulseStatus.activation.DEACTIVATED,
  runtime: PulseStatus.runtime.DISCONNECTED,
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const getRandomStatus = () => {
  const elements = [
    MOCK_PULSE_STATUS_CONNECTED,
    MOCK_PULSE_STATUS_CONNECTED,
    MOCK_PULSE_STATUS_ERROR,
    MOCK_PULSE_STATUS_DISCONNECTED,
    MOCK_PULSE_STATUS_DEACTIVATED,
  ]
  return elements[Math.floor(Math.random() * elements.length)]
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

  http.get('**/management/pulse/status', () => {
    return HttpResponse.json<PulseStatus>(MOCK_PULSE_STATUS_CONNECTED, { status: 200 })
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
