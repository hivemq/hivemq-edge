import { MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import type { Combiner, ManagedAsset, ManagedAssetList, ProtocolAdapter, PulseStatus } from '@/api/__generated__'
import { MOCK_CAPABILITY_PERSISTENCE, MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_PULSE_STATUS_CONNECTED, MOCK_PULSE_STATUS_DISCONNECTED } from '@/api/hooks/usePulse/__handlers__'
import {
  MOCK_PULSE_EXT_ASSET_MAPPERS_LIST,
  MOCK_PULSE_EXT_ASSETS_LIST,
  MOCK_PULSE_EXT_UNMAPPED_ASSETS_LIST,
} from '@/api/hooks/usePulse/__handlers__/pulse-mocks.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { factory, primaryKey } from '@mswjs/data'
import type { FactoryAPI } from '@mswjs/data/lib/glossary'
import type { PrimaryKey } from '@mswjs/data/lib/primaryKey'

type PrimaryKeyGetter = {
  id: PrimaryKey<string>
  json: StringConstructor
}

export type PulseFactory = FactoryAPI<{
  capabilities: PrimaryKeyGetter
  status: PrimaryKeyGetter
  assets: PrimaryKeyGetter
  assetMappers: PrimaryKeyGetter
}>

export const getPulseFactory = () =>
  factory({
    capabilities: {
      id: primaryKey(String),
      json: String,
    },
    status: {
      id: primaryKey(String),
      json: String,
    },
    assets: {
      id: primaryKey(String),
      json: String,
    },
    assetMappers: {
      id: primaryKey(String),
      json: String,
    },
  })

const interceptCapabilities = (factory: PulseFactory, isActivated: boolean) => {
  factory.capabilities.create({
    id: MOCK_CAPABILITY_PERSISTENCE.id,
    json: JSON.stringify(MOCK_CAPABILITY_PERSISTENCE),
  })

  if (isActivated) {
    factory.capabilities.create({
      id: MOCK_CAPABILITY_PULSE_ASSETS.id,
      json: JSON.stringify(MOCK_CAPABILITY_PULSE_ASSETS),
    })
  }

  cy.intercept<ProtocolAdapter>('GET', '/api/v1/frontend/capabilities', (req) => {
    const data = factory.capabilities.getAll()
    req.reply(200, { items: data.map((e) => JSON.parse(e.json)) })
  }).as('getCapabilities')

  cy.intercept<ProtocolAdapter>('POST', '/api/v1/management/pulse/activation-token', (req) => {
    // might already be there
    factory.capabilities.create({
      id: MOCK_CAPABILITY_PULSE_ASSETS.id,
      json: JSON.stringify(MOCK_CAPABILITY_PULSE_ASSETS),
    })
    req.reply(200)
  })
}

const interceptStatus = (factory: PulseFactory, isActivated: boolean) => {
  factory.status.create({
    id: QUERY_KEYS.PULSE_STATUS,
    json: JSON.stringify(isActivated ? MOCK_PULSE_STATUS_CONNECTED : MOCK_PULSE_STATUS_DISCONNECTED),
  })

  cy.intercept<PulseStatus>('GET', '/api/v1/management/pulse/status', (req) => {
    const data = factory.status.findFirst({
      where: {
        id: {
          equals: QUERY_KEYS.PULSE_STATUS,
        },
      },
    })
    req.reply(200, JSON.parse(data.json))
  }).as('getPulseStatus')
}

const interceptAssets = (factory: PulseFactory, isPreLoaded: boolean) => {
  const assetList = isPreLoaded ? MOCK_PULSE_EXT_ASSETS_LIST : MOCK_PULSE_EXT_UNMAPPED_ASSETS_LIST
  for (const asset of assetList.items) {
    factory.assets.create({
      id: asset.id,
      json: JSON.stringify(asset),
    })
  }

  cy.intercept<ManagedAssetList>('GET', '/api/v1/management/pulse/managed-assets', (req) => {
    const data = factory.assets.getAll()
    req.reply(200, { items: data.map((e) => JSON.parse(e.json)) })
  }).as('getAssets')

  cy.intercept<ManagedAsset>('PUT', '/api/v1/management/pulse/managed-assets/**', (req) => {
    const asset = req.body
    const updatedData = factory.assets.update({
      where: {
        id: {
          equals: asset.id,
        },
      },

      data: { json: JSON.stringify(asset) },
    })
    req.reply(200, { updated: JSON.parse(updatedData.json) })
  }).as('updateAsset')
}

const interceptAssetMappers = (factory: PulseFactory, isPreLoaded: boolean) => {
  if (isPreLoaded) {
    for (const assetMapper of MOCK_PULSE_EXT_ASSET_MAPPERS_LIST.items) {
      factory.assetMappers.create({
        id: assetMapper.id,
        json: JSON.stringify(assetMapper),
      })
    }
  }

  // make sure we have the connected adapters
  cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_OPC_UA] }).as('getProtocols')
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [
      { ...mockAdapter_OPCUA, id: 'my-adapter' },
      { ...mockAdapter_OPCUA, id: 'my-other-adapter' },
    ],
  }).as('getAdapters')

  cy.intercept('/api/v1/management/bridges', { items: [] }).as('getBridges')

  cy.intercept<ManagedAssetList>('GET', '/api/v1/management/pulse/asset-mappers', (req) => {
    const data = factory.assetMappers.getAll()
    req.reply(200, { items: data.map((e) => JSON.parse(e.json)) })
  }).as('getAssetMappers')

  cy.intercept<Combiner>('POST', '/api/v1/management/pulse/asset-mappers', (req) => {
    const combiner = req.body
    const newCombinerData = factory.assetMappers.create({
      id: combiner.id,
      json: JSON.stringify(combiner),
    })
    req.reply(200, { created: JSON.parse(newCombinerData.json) })
  }).as('createAssetMapper')
}

export const cy_interceptPulseWithMockDB = (factory: PulseFactory, isActivated = false, isPreLoaded = false) => {
  interceptCapabilities(factory, isActivated)
  interceptStatus(factory, isActivated)
  interceptAssets(factory, isPreLoaded)
  interceptAssetMappers(factory, isPreLoaded)
}
