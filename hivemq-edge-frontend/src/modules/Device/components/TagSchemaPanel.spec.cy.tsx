/// <reference types="cypress" />

import type { RJSFSchema } from '@rjsf/utils'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { TagSchemaPanel } from './TagSchemaPanel'

const mocTag = MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA)[0]

// The mocks mirror the real backend documents: the northbound document wraps the value in the non-writable
// envelope, the southbound document is value-only. The backend guarantees the value sub-schemas are
// identical for a plain value tag (pinned in TagSchemaCreationOutputImplSchemaBuilderTest).
const MOCK_VALUE_SCHEMA = GENERATE_DATA_MODELS(true, 'test')

const MOCK_NORTHBOUND_SCHEMA: RJSFSchema = {
  $schema: 'https://json-schema.org/draft/2019-09/schema',
  title: 'test',
  type: 'object',
  required: ['value'],
  properties: {
    tagName: { type: 'string', readOnly: true },
    timestamp: { type: 'integer', readOnly: true },
    value: MOCK_VALUE_SCHEMA,
  },
}

const MOCK_SOUTHBOUND_SCHEMA: RJSFSchema = {
  $schema: 'https://json-schema.org/draft/2019-09/schema',
  title: 'test',
  type: 'object',
  required: ['value'],
  properties: {
    value: MOCK_VALUE_SCHEMA,
  },
}

/**
 * Rebuilds a value with every object's members in the opposite insertion order, recursively. JSON object
 * member order carries no meaning, and the two directions are assembled independently, so a schema that only
 * differs this way is the same shape and must still collapse to a single panel.
 */
const reverseKeyOrder = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(reverseKeyOrder)
  if (value === null || typeof value !== 'object') return value
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .reverse()
      .map(([key, member]) => [key, reverseKeyOrder(member)])
  )
}

const MOCK_SOUTHBOUND_SCHEMA_REORDERED: RJSFSchema = {
  ...MOCK_SOUTHBOUND_SCHEMA,
  properties: { value: reverseKeyOrder(MOCK_VALUE_SCHEMA) as RJSFSchema },
}

// A tag whose southbound shape is not a projection of its northbound shape, e.g. an OPC-UA condition tag.
const MOCK_SOUTHBOUND_SCHEMA_INDEPENDENT: RJSFSchema = {
  $schema: 'https://json-schema.org/draft/2019-09/schema',
  title: 'test',
  type: 'object',
  required: ['value'],
  properties: {
    value: {
      type: 'object',
      title: 'writeModel',
      required: ['eventId', 'method'],
      properties: {
        eventId: { type: 'string', title: 'eventId' },
        method: { type: 'integer', title: 'method' },
        comment: { type: 'string', title: 'comment' },
      },
    },
  },
}

describe('TagSchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    // The northbound request carries no `direction` (it is the default); the southbound request is
    // intercepted per test.
    cy.intercept('/api/v1/management/protocol-adapters/schema/**', MOCK_NORTHBOUND_SCHEMA)
  })

  const interceptSouthboundSchema = (response: RJSFSchema | { statusCode: number }) =>
    cy.intercept(
      { method: 'GET', pathname: '**/protocol-adapters/schema/**', query: { direction: 'SOUTHBOUND' } },
      'statusCode' in response ? response : { body: response }
    )

  it('should render properly', () => {
    interceptSouthboundSchema(MOCK_SOUTHBOUND_SCHEMA)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.getByTestId('tag-schema-header').should('have.text', 'Tag')
    cy.getByTestId('topic-wrapper').should('have.text', formatTopicString('opcua-1/power/off'))
    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Current schema')
      cy.get('h3').should('have.text', 'test')
      cy.get('[role="list"] li').should('length', 11)
      cy.get('#tag-schema-panel-helptext').should('have.text', 'Both directions use the same schema for this tag.')
    })
    cy.getByTestId('tag-schema-download').should('have.text', 'Download the schema').should('not.be.disabled')
  })

  it('should show a single schema when the northbound and southbound value shapes are identical', () => {
    interceptSouthboundSchema(MOCK_SOUTHBOUND_SCHEMA)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    // The documents differ (envelope vs value-only) but the value shapes match, so only one panel renders.
    cy.getByTestId('tag-schema-northbound').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Current schema')
    cy.getByTestId('tag-schema-panel-southbound').should('not.exist')
  })

  it('should show a single schema when the value shapes differ only in member order', () => {
    interceptSouthboundSchema(MOCK_SOUTHBOUND_SCHEMA_REORDERED)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    // Same shape, different insertion order — a stringify-based comparison would split this into two panels
    // and tell the user the two directions differ, which is false.
    cy.getByTestId('tag-schema-northbound').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Current schema')
    cy.getByTestId('tag-schema-panel-southbound').should('not.exist')
  })

  it('should show both schemas when the southbound shape is independent of the northbound shape', () => {
    interceptSouthboundSchema(MOCK_SOUTHBOUND_SCHEMA_INDEPENDENT)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Northbound schema')
      cy.get('#tag-schema-panel-helptext').should(
        'have.text',
        'The data published for this tag. A valid source for data transformation.'
      )
    })

    cy.getByTestId('tag-schema-panel-southbound').within(() => {
      cy.get('label').should('have.text', 'Southbound schema')
      cy.get('#tag-schema-panel-southbound-helptext').should(
        'have.text',
        'The shape of a southbound message to this tag. Whether an individual field can be written is shown per field.'
      )
    })
  })

  it('should render the northbound schema without waiting for a slow southbound request', () => {
    // The two queries are independent: the northbound panel must appear while the southbound request is
    // still in flight.
    cy.intercept(
      { method: 'GET', pathname: '**/protocol-adapters/schema/**', query: { direction: 'SOUTHBOUND' } },
      { body: MOCK_SOUTHBOUND_SCHEMA, delay: 8000 }
    )
    // Distinct adapterId: the query client is shared across tests, and this test deliberately leaves an
    // 8s-pending southbound request behind — a colliding query key would leak it into the next test.
    cy.mountWithProviders(<TagSchemaPanel adapterId="test-slow" tag={mocTag} />)

    // Asserted well inside the 8s southbound delay (default 4s command timeout): the northbound panel is up
    // while the southbound query still loads.
    cy.getByTestId('tag-schema-northbound').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Northbound schema')
  })

  it('should still render the northbound schema and an error when the southbound schema cannot be loaded', () => {
    interceptSouthboundSchema({ statusCode: 500 })
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    // The southbound failure must be visible, not silently swallowed into a half-answer.
    cy.getByTestId('tag-schema-northbound').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Northbound schema')
    cy.get('[role="alert"]').should('contain.text', 'Cannot load the southbound schema')
    cy.getByTestId('tag-schema-panel-southbound').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    interceptSouthboundSchema(MOCK_SOUTHBOUND_SCHEMA)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.checkAccessibility()
  })
})
