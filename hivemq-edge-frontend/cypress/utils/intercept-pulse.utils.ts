import { factory, primaryKey } from '@mswjs/data'
import type { FactoryAPI } from '@mswjs/data/lib/glossary'
import type { PrimaryKey } from '@mswjs/data/lib/primaryKey'

import type { ProtocolAdapter } from '@/api/__generated__'
import { MOCK_CAPABILITY_PERSISTENCE, MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'

type PrimaryKeyGetter = {
  id: PrimaryKey<string>
  json: StringConstructor
}

export type PulseFactory = FactoryAPI<{
  capabilities: PrimaryKeyGetter
  assets: PrimaryKeyGetter
  assetMappers: PrimaryKeyGetter
}>

export const getPulseFactory = () =>
  factory({
    capabilities: {
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

export const cy_interceptPulseWithMockDB = (factory: PulseFactory, isActivated = false) => {
  interceptCapabilities(factory, isActivated)
  // interceptAssets(factory)
  // interceptAssetMappers(factory)
}
