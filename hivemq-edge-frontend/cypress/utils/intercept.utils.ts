import type { FactoryAPI } from '@mswjs/data/lib/glossary'
import type { PrimaryKey } from '@mswjs/data/lib/primaryKey'
import { v4 as uuidv4 } from 'uuid'

import { MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import type {
  BehaviorPolicy,
  BehaviorPolicyList,
  Bridge,
  DataPolicy,
  DataPolicyList,
  PolicySchema,
  ProtocolAdapter,
  SchemaList,
  Script,
  ScriptList,
} from '@/api/__generated__'
import { mockAuthApi, mockValidCredentials } from '@/api/hooks/usePostAuthentication/__handlers__'
import { mockGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'

export const cy_interceptCoreE2E = () => {
  // requests sent but not necessary to logic
  cy.intercept('https://api.github.com/repos/hivemq/hivemq-edge/releases', { statusCode: 202, log: false })
  cy.intercept('/api/v1/frontend/notifications', { statusCode: 202, log: false })
  cy.intercept('/api/v1/management/protocol-adapters/status', { statusCode: 202, log: false })
  cy.intercept('/api/v1/management/bridges/status', { statusCode: 202, log: false })
  cy.intercept('/api/v1/frontend/capabilities', { statusCode: 202, log: false })

  // code business requests
  cy.intercept('/api/v1/auth/authenticate', mockAuthApi(mockValidCredentials))
  cy.intercept('/api/v1/frontend/configuration', mockGatewayConfiguration)

  // Add a dummy element so we can check uniqueness
  cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] }).as('getAdapters')
}

type PrimaryKeyGetter = {
  id: PrimaryKey<string>
  json: StringConstructor
}

export type EdgeFactory = FactoryAPI<{
  bridge?: PrimaryKeyGetter
  eventLog?: PrimaryKeyGetter
  adapter?: PrimaryKeyGetter
}>

export type DataHubFactory = FactoryAPI<{
  dataPolicy: PrimaryKeyGetter
  behaviourPolicy: PrimaryKeyGetter
  schema: PrimaryKeyGetter
  script: PrimaryKeyGetter
}>

const interceptBridges = (factory: EdgeFactory) => {
  cy.intercept('GET', '/api/v1/management/bridges', (req) => {
    const allBridgeData = factory.bridge.getAll()
    const allBridges = allBridgeData.map<Bridge>((data) => ({ ...JSON.parse(data.json) }))
    req.reply(200, { items: allBridges })
  })

  cy.intercept<Bridge>('POST', '/api/v1/management/bridges', (req) => {
    const bridge = req.body
    const newBridgeData = factory.bridge.create({
      id: bridge.id,
      json: JSON.stringify(bridge),
    })

    if (factory.eventLog) {
      const dateNow = Date.now()
      const uuid = uuidv4()

      // create the event in the mock database
      factory.eventLog.create({
        id: uuid,
        json: JSON.stringify({
          identifier: {
            type: 'EVENT',
            identifier: uuid,
          },
          severity: 'ERROR',
          message: `Bridge '${bridge.id}' disconnected`,
          payload: {
            contentType: 'PLAIN_TEXT',
            content:
              'com.hivemq.client.mqtt.exceptions.ConnectionClosedException: Server closed connection without DISCONNECT.',
          },
          created: new Date(dateNow).toISOString(),
          timestamp: dateNow,
          source: {
            type: 'BRIDGE',
            identifier: bridge.id,
          },
        }),
      })
    }

    req.reply(200, newBridgeData)
  })

  cy.intercept<Bridge>('PUT', '/api/v1/management/bridges/**', (req) => {
    const bridge = req.body

    factory.bridge.update({
      where: {
        id: {
          equals: bridge.id,
        },
      },

      data: { json: JSON.stringify(bridge) },
    })

    req.reply(200, '')
  })

  cy.intercept<Bridge>('DELETE', '/api/v1/management/bridges/**', (req) => {
    const urlParts = req.url.split('/')
    const bridgeId = urlParts[urlParts.length - 1]

    factory.bridge.delete({
      where: {
        id: {
          equals: bridgeId,
        },
      },
    })
    req.reply(200, '')
  }).as('deleteBridge')
}

const interceptAdapters = (factory: EdgeFactory) => {
  cy.intercept<ProtocolAdapter>('GET', '/api/v1/management/protocol-adapters/adapters', (req) => {
    const allBridgeData = factory.adapter.getAll()
    const allBridges = allBridgeData.map<ProtocolAdapter>((data) => ({ ...JSON.parse(data.json) }))
    req.reply(200, { items: allBridges })
  })

  cy.intercept<ProtocolAdapter>('POST', '/api/v1/management/protocol-adapters/adapters/opcua', (req) => {
    const newAdapter = req.body
    const newAdapterData = factory.adapter.create({
      id: newAdapter.id,
      json: JSON.stringify(newAdapter),
    })

    if (factory.eventLog) {
      const dateNow = Date.now()
      const uuid = uuidv4()
      // create the event in the mock database
      factory.eventLog.create({
        id: uuid,
        json: JSON.stringify({
          identifier: {
            type: 'EVENT',
            identifier: uuidv4(),
          },
          severity: 'INFO',
          message: `Adapter '${newAdapter.id}' started OK.`,
          created: new Date(dateNow).toISOString(),
          timestamp: dateNow,
          associatedObject: {
            type: 'ADAPTER_TYPE',
            identifier: 'opcua',
          },
          source: {
            type: 'ADAPTER',
            identifier: newAdapter.id,
          },
        }),
      })
    }

    req.reply(200, newAdapterData)
  })
}

export const cy_interceptWithMockDB = (factory: EdgeFactory) => {
  if (factory.bridge) {
    interceptBridges(factory)
  }

  if (factory.adapter) {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_OPC_UA] }).as('getProtocols')
    interceptAdapters(factory)
  }

  if (factory.eventLog) {
    cy.intercept('GET', '/api/v1/management/events?*', (req) => {
      const allEventData = factory.eventLog.getAll()
      const allEvents = allEventData.map<Bridge>((data) => ({ ...JSON.parse(data.json) }))
      req.reply(200, { items: allEvents })
    })
  }
}

export const cy_interceptDataHubWithMockDB = (factory: DataHubFactory) => {
  cy.intercept<DataPolicyList>('GET', '/api/v1/data-hub/data-validation/policies', (req) => {
    const dataPolicies = factory.dataPolicy.getAll()
    const allDataPolicies = dataPolicies.map<DataPolicy>((data) => ({ ...JSON.parse(data.json) }))
    req.reply(200, { items: allDataPolicies })
  })

  cy.intercept<DataPolicyList>('GET', '/api/v1/data-hub/data-validation/policies/**', (req) => {
    const urlParts = req.url.split('/')
    const policyId = urlParts[urlParts.length - 1]

    const data = factory.dataPolicy.findFirst({
      where: {
        id: {
          equals: policyId,
        },
      },
    })

    const dataPolicy = JSON.parse(data.json)
    req.reply(200, dataPolicy)
  })

  cy.intercept<BehaviorPolicyList>('GET', '/api/v1/data-hub/behavior-validation/policies', (req) => {
    const dataPolicies = factory.behaviourPolicy.getAll()
    const allDataPolicies = dataPolicies.map<BehaviorPolicy>((data) => ({ ...JSON.parse(data.json) }))
    req.reply(200, { items: allDataPolicies })
  })

  cy.intercept<SchemaList>('GET', '/api/v1/data-hub/schemas', (req) => {
    const data = factory.schema.getAll()
    const allSchemas = data.map<PolicySchema>((data) => ({ ...JSON.parse(data.json) }))
    req.reply(200, { items: allSchemas })
  })

  cy.intercept<ScriptList>('GET', '/api/v1/data-hub/scripts', (req) => {
    const data = factory.script.getAll()
    const allScripts = data.map<Script>((data) => ({ ...JSON.parse(data.json) }))
    req.reply(200, { items: allScripts })
  })
}
