/// <reference types="cypress" />

import type { RJSFSchema } from '@rjsf/utils'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { TagSchemaPanel } from './TagSchemaPanel'

const mocTag = MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA)[0]

// The mocks mirror the real backend documents: the read (northbound) document wraps the value in the
// non-writable envelope, the write (southbound) document is value-only. The backend guarantees the value
// sub-schemas are identical for a plain value tag (pinned in TagSchemaCreationOutputImplSchemaBuilderTest).
const MOCK_VALUE_SCHEMA = GENERATE_DATA_MODELS(true, 'test')

const MOCK_READ_SCHEMA: RJSFSchema = {
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

const MOCK_WRITE_SCHEMA: RJSFSchema = {
  $schema: 'https://json-schema.org/draft/2019-09/schema',
  title: 'test',
  type: 'object',
  required: ['value'],
  properties: {
    value: MOCK_VALUE_SCHEMA,
  },
}

// A tag whose write shape is not a projection of its read shape, e.g. an OPC-UA condition tag.
const MOCK_WRITE_SCHEMA_INDEPENDENT: RJSFSchema = {
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
    // The read request carries no `direction` (it is the default); the write request is intercepted per test.
    cy.intercept('/api/v1/management/protocol-adapters/schema/**', MOCK_READ_SCHEMA)
  })

  const interceptWriteSchema = (response: RJSFSchema | { statusCode: number }) =>
    cy.intercept(
      { method: 'GET', pathname: '**/protocol-adapters/schema/**', query: { direction: 'SOUTHBOUND' } },
      'statusCode' in response ? response : { body: response }
    )

  it('should render properly', () => {
    interceptWriteSchema(MOCK_WRITE_SCHEMA)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.getByTestId('tag-schema-header').should('have.text', 'Tag')
    cy.getByTestId('topic-wrapper').should('have.text', formatTopicString('opcua-1/power/off'))
    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Current schema')
      cy.get('h3').should('have.text', 'test')
      cy.get('[role="list"] li').should('length', 11)
      cy.get('#tag-schema-panel-helptext').should('have.text', 'Reading and writing this tag use the same schema.')
    })
    cy.getByTestId('tag-schema-download').should('have.text', 'Download the schema').should('not.be.disabled')
  })

  it('should show a single schema when the read and write value shapes are identical', () => {
    interceptWriteSchema(MOCK_WRITE_SCHEMA)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    // The documents differ (envelope vs value-only) but the value shapes match, so only one panel renders.
    cy.getByTestId('tag-schema-read').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Current schema')
    cy.getByTestId('tag-schema-panel-write').should('not.exist')
  })

  it('should show both schemas when the write shape is independent of the read shape', () => {
    interceptWriteSchema(MOCK_WRITE_SCHEMA_INDEPENDENT)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.getByTestId('tag-schema-panel').within(() => {
      cy.get('label').should('have.text', 'Read schema')
      cy.get('#tag-schema-panel-helptext').should(
        'have.text',
        'The data published for this tag. A valid source for data transformation.'
      )
    })

    cy.getByTestId('tag-schema-panel-write').within(() => {
      cy.get('label').should('have.text', 'Write schema')
      cy.get('#tag-schema-panel-write-helptext').should(
        'have.text',
        'The shape of a write to this tag. Whether an individual field can be written is shown per field.'
      )
    })
  })

  it('should render the read schema without waiting for a slow write request', () => {
    // The two queries are independent: the read panel must appear while the write request is still in flight.
    cy.intercept(
      { method: 'GET', pathname: '**/protocol-adapters/schema/**', query: { direction: 'SOUTHBOUND' } },
      { body: MOCK_WRITE_SCHEMA, delay: 8000 }
    )
    // Distinct adapterId: the query client is shared across tests, and this test deliberately leaves an
    // 8s-pending write request behind — a colliding query key would leak it into the next test.
    cy.mountWithProviders(<TagSchemaPanel adapterId="test-slow" tag={mocTag} />)

    // Asserted well inside the 8s write delay (default 4s command timeout): the read panel is up while the
    // write query still loads.
    cy.getByTestId('tag-schema-read').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Read schema')
  })

  it('should still render the read schema and an error when the write schema cannot be loaded', () => {
    interceptWriteSchema({ statusCode: 500 })
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    // The write failure must be visible, not silently swallowed into a half-answer.
    cy.getByTestId('tag-schema-read').should('be.visible')
    cy.getByTestId('tag-schema-panel').find('label').should('have.text', 'Read schema')
    cy.get('[role="alert"]').should('contain.text', 'Cannot load the write schema')
    cy.getByTestId('tag-schema-panel-write').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    interceptWriteSchema(MOCK_WRITE_SCHEMA)
    cy.mountWithProviders(<TagSchemaPanel adapterId="test" tag={mocTag} />)

    cy.checkAccessibility()
  })
})
