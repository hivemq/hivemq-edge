/// <reference types="cypress" />

import type { UiSchema } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import { mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import { combinerMappingJsonSchema } from '@/api/schemas/combiner-mapping.json-schema'
import type { DataCombining, DomainTag, DomainTagList } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import { mockAdapter_OPCUA, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import type { CombinerContext, EntityQuery } from '@/modules/Mappings/types'
import { encodeDataUriJsonSchema } from '@/modules/TopicFilters/utils/topic-filter.schema'

import { DataCombiningEditorField } from './DataCombiningEditorField'

const mockDataCombiningTableSchema = { ...combinerMappingJsonSchema, $ref: '#/definitions/DataCombining' }

const mockDataCombiningTableUISchema: UiSchema = {
  sources: {
    'ui:title': 'Sources',
    primary: {
      'ui:title': 'Primary',
      'ui:description': 'The text below the primary selector',
    },
  },
  destination: {
    'ui:title': 'Destination',
    topic: {
      'ui:title': 'Topic',
      'ui:description': 'The text below the selector',
    },
    schema: {
      'ui:title': 'Destination schema',
      'ui:description': 'The text below the schema browser',
    },
  },
  'ui:field': DataCombiningEditorField,
}

const mockFormData: DataCombining = {
  id: '58677276-fc48-4a9a-880c-41c755f5063b',
  sources: {
    primary: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG },
    tags: ['my/tag/t1', 'my/tag/t3'],
    topicFilters: ['my/topic/+/temp'],
  },
  destination: { topic: 'my/topic' },
  instructions: [],
}

describe('DataCombiningEditorField', () => {
  beforeEach(() => {
    cy.viewport(1000, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] })
    cy.intercept('/api/v1/management/bridges', { statusCode: 404 })
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={mockCombinerMapping}
      />
    )

    cy.getByTestId('combining-editor-source-header').should('contain.text', 'Sources')

    cy.getByTestId('combining-editor-destination-header').should('contain.text', 'Destination')

    cy.getByTestId('combining-editor-sources-attributes').within(() => {
      cy.get('[role="group"] label').should('have.text', 'Integration points')
      cy.get('[role="group"] label + div').within(() => {
        //TODO[TEXT] The ReactSelect component need a better distribution of data-testid
      })
      //TODO[TEXT] Relies on the selector component to have been tested properly
      cy.get('[role="group"] label + div').should('contain.text', 'my/tag/t1')
      cy.get('[role="group"] label + div').should('contain.text', 'my/tag/t3')
      cy.get('[role="group"] label + div').should('contain.text', 'my/topic/+/temp')

      cy.get('[role="group"] label + div + div').should(
        'have.text',
        'Select the tags and topic filters you want to use for combining'
      )
    })

    cy.getByTestId('combining-editor-destination-topic').within(() => {
      cy.get('[role="group"] label').should('have.text', 'Topic')
      cy.get('[role="group"] label + div').should('contain.text', 'my/topic')
      cy.get('[role="group"] label + div + div').should('have.text', 'The text below the selector')
    })

    cy.getByTestId('combining-editor-destination-topic').within(() => {
      cy.get('[role="group"] label').should('have.text', 'Topic')
      cy.get('[role="group"] label + div').should('contain.text', 'my/topic')
      cy.get('[role="group"] label + div + div').should('have.text', 'The text below the selector')
    })

    cy.getByTestId('combining-editor-sources-schemas').within(() => {
      cy.get('[role="group"] label').should('have.text', 'Source schemas')
      // mockCombinerMapping now has 3 instructions → 3 schema panels are shown (with warnings since no schema URLs intercepted)
      cy.getByTestId('topic-wrapper').should('have.length', 3)
      cy.get('[role="group"] label + div + div').should(
        'have.text',
        'Drag the required properties into the destination schema'
      )
    })

    cy.getByTestId('combining-editor-destination-schema').within(() => {
      cy.get('[role="group"] label').should('have.text', 'Destination schema')
      cy.get('[role="alert"]').should('have.text', 'There are no schemas available yet')
      cy.get('[role="group"] div[id$="-helptext"]').should('have.text', 'The text below the schema browser')
    })

    cy.getByTestId('combining-editor-sources-primary').within(() => {
      cy.get('[role="group"] label').should('have.text', 'Primary')
      // Selected primary is now rendered as a scoped PLCTag badge: "my-adapter :: my/tag/t1"
      cy.get('[role="group"] label + div [data-testid="topic-wrapper"]').should('be.visible')
      cy.get('[role="group"] label + div [data-testid="topic-wrapper"]').should('contain.text', 'my-adapter')
      cy.get('[role="group"] label + div [data-testid="topic-wrapper"]').should('contain.text', 't1')
      cy.get('[role="group"] label + div + div').should('have.text', 'The text below the primary selector')
    })
  })

  it.skip('should create a mapping properly', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={mockCombinerMapping}
      />
    )

    // TODO[NVL] add the tests
  })

  it.skip('should render the schema handlers', () => {
    // TODO[NVL] add the tests
  })

  it('should handle queries not loaded initially (race condition fix)', () => {
    // Helper to create mock query
    const mockTagQuery = (tags: string[], isLoaded: boolean): Partial<UseQueryResult<DomainTagList, Error>> => {
      if (isLoaded) {
        return {
          data: {
            items: tags.map((name) => ({ name, description: `${name} description` }) as DomainTag),
          },
          isLoading: false,
          isSuccess: true,
          isError: false,
        }
      }
      return {
        data: undefined,
        isLoading: true,
        isSuccess: false,
        isError: false,
      }
    }

    const formDataWithTags: DataCombining = {
      id: 'test-mapping-1',
      sources: {
        primary: { id: 'tag1', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
        tags: ['tag1', 'tag2'], // tag2 needs context lookup
        topicFilters: [],
      },
      destination: { topic: 'my/topic' },
      instructions: [
        {
          sourceRef: { id: 'tag1', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
          destination: 'dest1',
          source: 'tag1',
        },
      ],
    }

    // Test with queries NOT loaded
    const contextQueriesNotLoaded: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'adapter-1', type: EntityType.ADAPTER },
          query: mockTagQuery(['tag1', 'tag2'], false) as UseQueryResult<DomainTagList, Error>,
        },
      ],
    }

    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={formDataWithTags}
        formContext={contextQueriesNotLoaded}
      />
    )

    // Component should render even when queries not loaded
    cy.getByTestId('combining-editor-sources-attributes').should('exist')
    cy.getByTestId('combining-editor-destination-topic').should('exist')
  })

  it('should handle queries loaded (race condition fix)', () => {
    // Helper to create mock query
    const mockTagQuery = (tags: string[], isLoaded: boolean): Partial<UseQueryResult<DomainTagList, Error>> => {
      if (isLoaded) {
        return {
          data: {
            items: tags.map((name) => ({ name, description: `${name} description` }) as DomainTag),
          },
          isLoading: false,
          isSuccess: true,
          isError: false,
        }
      }
      return {
        data: undefined,
        isLoading: true,
        isSuccess: false,
        isError: false,
      }
    }

    const formDataWithTags: DataCombining = {
      id: 'test-mapping-2',
      sources: {
        primary: { id: 'tag1', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
        tags: ['tag1', 'tag2'],
        topicFilters: [],
      },
      destination: { topic: 'my/topic' },
      instructions: [
        {
          sourceRef: { id: 'tag1', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
          destination: 'dest1',
          source: 'tag1',
        },
      ],
    }

    // Test with queries LOADED
    const contextQueriesLoaded: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'adapter-1', type: EntityType.ADAPTER },
          query: mockTagQuery(['tag1', 'tag2'], true) as UseQueryResult<DomainTagList, Error>,
        },
      ],
    }

    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={formDataWithTags}
        formContext={contextQueriesLoaded}
      />
    )

    // Component should render successfully with loaded queries
    cy.getByTestId('combining-editor-sources-attributes').should('exist')
    cy.getByTestId('combining-editor-destination-topic').should('exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={mockFormData}
      />
    )
    cy.checkAccessibility()
  })

  // ---------------------------------------------------------------------------
  // EDG-164: sources.tags deprecation — integration points must be built
  //          from instructions.sourceRef, not from the deprecated tags array
  // ---------------------------------------------------------------------------
  describe('EDG-164 — integration points derive from instructions, not sources.tags', () => {
    const writingSchemaUrl = (adapterId: string, tagId: string) =>
      `/api/v1/management/protocol-adapters/writing-schema/${adapterId}/${encodeURIComponent(tagId)}`

    const sensorSchema = { type: 'object', properties: { value: { type: 'number' } } }

    /** A loaded entityQuery for a single adapter */
    const loadedTagQuery = (adapterId: string, tagNames: string[]): EntityQuery => ({
      entity: { id: adapterId, type: EntityType.ADAPTER },
      query: {
        data: { items: tagNames.map((name) => ({ name, description: `${name} desc` })) },
        isLoading: false,
        isSuccess: true,
        isError: false,
      } as UseQueryResult<DomainTagList, Error>,
    })

    beforeEach(() => {
      cy.viewport(1400, 900)
    })

    // -------------------------------------------------------------------------
    // Scenario 1: fresh mapping — no instructions, no deprecated tags
    // Expected: no chips selected, no schemas loaded
    // -------------------------------------------------------------------------
    it('S1: fresh mapping with no instructions shows empty integration points', () => {
      const formData: DataCombining = {
        id: 'edg164-s1',
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: [],
          topicFilters: [],
        },
        destination: { topic: 'out/topic' },
        instructions: [],
      }

      const formContext: CombinerContext = {
        entityQueries: [loadedTagQuery('adapter-a', ['temperature', 'pressure'])],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      // No chips in the multi-select (chips render as topic-wrapper via PLCTag/EntityTag)
      cy.getByTestId('combining-editor-sources-attributes').within(() => {
        cy.getByTestId('topic-wrapper').should('not.exist')
      })

      // No schema panels loaded
      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('not.exist')
        cy.get('[role="alert"]').should('contain.text', 'There are no schemas available yet')
      })
    })

    // -------------------------------------------------------------------------
    // Scenario 2: single adapter, scoped instruction
    // Expected: one chip `adapter-a :: temperature`, one schema panel
    // -------------------------------------------------------------------------
    it('S2: scoped instruction drives one chip and one schema panel', () => {
      cy.intercept('GET', writingSchemaUrl('adapter-a', 'temperature'), {
        statusCode: 200,
        body: sensorSchema,
      }).as('schema')

      const formData: DataCombining = {
        id: 'edg164-s2',
        sources: {
          primary: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-a' },
          tags: ['temperature'],
          topicFilters: [],
        },
        destination: { topic: 'out/topic' },
        instructions: [
          {
            sourceRef: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-a' },
            source: '$.value',
            destination: '$.out',
          },
        ],
      }

      const formContext: CombinerContext = {
        entityQueries: [loadedTagQuery('adapter-a', ['temperature', 'pressure'])],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      cy.wait('@schema')

      // Exactly one chip, showing ownership
      cy.getByTestId('combining-editor-sources-attributes').should('contain.text', 'adapter-a :: temperature')

      // Exactly one schema panel, headed by the adapter
      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 1)
        cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'adapter-a')
      })
    })

    // -------------------------------------------------------------------------
    // Scenario 3 (main bug): same-named tag from two adapters
    // BEFORE fix: both instructions resolve to same scope → one chip, one schema
    // AFTER fix:  distinct scopes → two chips, two schemas
    // -------------------------------------------------------------------------
    it('S3 (bug): same-named tag from two adapters shows two distinct chips and loads two schemas', () => {
      cy.intercept('GET', writingSchemaUrl('o1', 'temperature'), {
        statusCode: 200,
        body: { type: 'object', properties: { value: { type: 'number' } } },
      }).as('schemaO1')

      cy.intercept('GET', writingSchemaUrl('o2', 'temperature'), {
        statusCode: 200,
        body: { type: 'object', properties: { value: { type: 'string' } } },
      }).as('schemaO2')

      const formData: DataCombining = {
        id: 'edg164-s3',
        sources: {
          primary: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'o1' },
          // Deprecated array — deliberately contains two identical IDs (the problematic state)
          tags: ['temperature', 'temperature'],
          topicFilters: [],
        },
        destination: { topic: 'out/topic' },
        instructions: [
          // Two instructions, same tag name, different scopes — the authoritative truth
          {
            sourceRef: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'o1' },
            source: '$.value',
            destination: '$.o1_value',
          },
          {
            sourceRef: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'o2' },
            source: '$.value',
            destination: '$.o2_value',
          },
        ],
      }

      const formContext: CombinerContext = {
        entityQueries: [loadedTagQuery('o1', ['temperature']), loadedTagQuery('o2', ['temperature'])],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      cy.wait(['@schemaO1', '@schemaO2'])

      // Two distinct ownership chips — no unscoped 'temperature' chip
      cy.getByTestId('combining-editor-sources-attributes').within(() => {
        cy.contains('o1 :: temperature').should('exist')
        cy.contains('o2 :: temperature').should('exist')
        // No bare 'temperature' chip (unscoped — the bug)
        // Chips render as topic-wrapper via PLCTag/EntityTag (not with CSS class "multiValue")
        cy.getByTestId('topic-wrapper').should('have.length', 2)
      })

      // Two schema panels — one per adapter
      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 2)
        cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'o1')
        cy.getByTestId('topic-wrapper').eq(1).should('contain.text', 'o2')
      })
    })

    // -------------------------------------------------------------------------
    // Scenario 4: old deprecated data — sources.tags present, no instructions
    // Expected: empty selection (deprecated data must be ignored)
    // -------------------------------------------------------------------------
    it('S4: old deprecated sources.tags with no instructions results in empty selection', () => {
      const formData: DataCombining = {
        id: 'edg164-s4',
        sources: {
          primary: { id: 'temperature', type: DataIdentifierReference.type.TAG },
          // Legacy data: tags array populated, but no scope, no instructions
          tags: ['temperature', 'pressure'],
          topicFilters: [],
        },
        destination: { topic: 'out/topic' },
        instructions: [],
      }

      const formContext: CombinerContext = {
        entityQueries: [loadedTagQuery('adapter-a', ['temperature', 'pressure'])],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      // No chips — deprecated sources.tags must be ignored without instructions
      cy.getByTestId('combining-editor-sources-attributes').within(() => {
        cy.getByTestId('topic-wrapper').should('not.exist')
      })

      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('not.exist')
      })
    })

    // -------------------------------------------------------------------------
    // Scenario 5: old deprecated data WITH some instructions
    // Expected: only the instruction-sourced chips appear (not all from sources.tags)
    // -------------------------------------------------------------------------
    it('S5: partial deprecated data — only instruction-based sources shown', () => {
      cy.intercept('GET', writingSchemaUrl('adapter-a', 'temperature'), {
        statusCode: 200,
        body: sensorSchema,
      }).as('schema')

      const formData: DataCombining = {
        id: 'edg164-s5',
        sources: {
          primary: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-a' },
          // Deprecated: three tags, but only one has a scoped instruction
          tags: ['temperature', 'pressure', 'humidity'],
          topicFilters: [],
        },
        destination: { topic: 'out/topic' },
        instructions: [
          // Only temperature has a scoped instruction
          {
            sourceRef: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-a' },
            source: '$.value',
            destination: '$.out',
          },
        ],
      }

      const formContext: CombinerContext = {
        entityQueries: [loadedTagQuery('adapter-a', ['temperature', 'pressure', 'humidity'])],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      cy.wait('@schema')

      // Only the instruction-sourced tag appears — pressure and humidity are ignored
      cy.getByTestId('combining-editor-sources-attributes').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 1)
        cy.contains('adapter-a :: temperature').should('exist')
        cy.contains('pressure').should('not.exist')
        cy.contains('humidity').should('not.exist')
      })

      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 1)
      })
    })

    // -------------------------------------------------------------------------
    // Scenario 6: topic filter with instruction
    // Expected: topic filter chip shown, schema loads
    // -------------------------------------------------------------------------
    it('S6: topic filter with scoped instruction shows chip and loads schema', () => {
      cy.intercept('GET', '/api/v1/management/topic-filters/schema?topicFilter=plant%2F%2B%2Ftemp', {
        statusCode: 200,
        body: sensorSchema,
      }).as('tfSchema')

      const formData: DataCombining = {
        id: 'edg164-s6',
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: [],
          topicFilters: ['plant/+/temp'],
        },
        destination: { topic: 'out/topic' },
        instructions: [
          {
            sourceRef: { id: 'plant/+/temp', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null },
            source: '$.value',
            destination: '$.out',
          },
        ],
      }

      const formContext: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'broker', type: EntityType.EDGE_BROKER },
            query: {
              data: { items: [{ topicFilter: 'plant/+/temp', description: 'Plant sensors' }] },
              isLoading: false,
              isSuccess: true,
              isError: false,
            } as unknown as UseQueryResult<DomainTagList, Error>,
          },
        ],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      // Topic filter chip present
      cy.getByTestId('combining-editor-sources-attributes').should('contain.text', 'plant/+/temp')
    })

    // -------------------------------------------------------------------------
    // Scenario 7: mixed tags + topic filters
    // Expected: all schema panels load
    // -------------------------------------------------------------------------
    it('S7: mixed tags and topic filters from instructions all load schemas', () => {
      cy.intercept('GET', writingSchemaUrl('adapter-a', 'temperature'), {
        statusCode: 200,
        body: sensorSchema,
      }).as('tagSchema')

      const formData: DataCombining = {
        id: 'edg164-s7',
        sources: {
          primary: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-a' },
          tags: ['temperature'],
          topicFilters: ['plant/+/temp'],
        },
        destination: { topic: 'out/topic' },
        instructions: [
          {
            sourceRef: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-a' },
            source: '$.value',
            destination: '$.tag_value',
          },
          {
            sourceRef: { id: 'plant/+/temp', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null },
            source: '$.reading',
            destination: '$.tf_reading',
          },
        ],
      }

      const formContext: CombinerContext = {
        entityQueries: [
          loadedTagQuery('adapter-a', ['temperature']),
          {
            entity: { id: 'broker', type: EntityType.EDGE_BROKER },
            query: {
              data: { items: [{ topicFilter: 'plant/+/temp', description: 'Plant sensors' }] },
              isLoading: false,
              isSuccess: true,
              isError: false,
            } as unknown as UseQueryResult<DomainTagList, Error>,
          },
        ],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
          formContext={formContext}
        />
      )

      cy.wait('@tagSchema')

      // Both chips present
      cy.getByTestId('combining-editor-sources-attributes').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 2)
        cy.contains('adapter-a :: temperature').should('exist')
        cy.contains('plant/+/temp').should('exist')
      })

      // Two schema panels
      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 2)
      })
    })
  })

  describe('Blog Post Screenshots — Tag Ownership Display', { tags: ['@percy'] }, () => {
    const boilerSourceSchema = {
      type: 'object',
      title: 'Boiler Sensor Data',
      properties: {
        temperature: { type: 'number', title: 'Temperature', description: '°C' },
        pressure: { type: 'number', title: 'Pressure', description: 'bar' },
        'flow-rate': { type: 'number', title: 'Flow Rate', description: 'L/min' },
      },
    }

    const destinationSchema = encodeDataUriJsonSchema({
      type: 'object',
      title: 'Combined Output',
      properties: {
        line1_temperature: { type: 'number', title: 'Line 1 Temperature' },
        line2_temperature: { type: 'number', title: 'Line 2 Temperature' },
        line2_pressure: { type: 'number', title: 'Line 2 Pressure' },
      },
    })

    beforeEach(() => {
      cy.viewport(1400, 1016)
    })

    it('should capture ownership chips in source and destination panels', () => {
      cy.intercept('GET', '**/writing-schema/boiler-line-1/**', { body: boilerSourceSchema }).as('schema1')
      cy.intercept('GET', '**/writing-schema/boiler-line-2/**', { body: boilerSourceSchema }).as('schema2')

      const formData: DataCombining = {
        id: 'blog-screenshot-combiner',
        sources: {
          primary: { id: 'boiler/temperature', type: DataIdentifierReference.type.TAG, scope: 'boiler-line-1' },
          tags: ['boiler/temperature', 'boiler/pressure'],
          topicFilters: [],
        },
        destination: {
          topic: 'plant/combined/output',
          schema: destinationSchema,
        },
        instructions: [
          {
            sourceRef: { id: 'boiler/temperature', type: DataIdentifierReference.type.TAG, scope: 'boiler-line-1' },
            source: '$.temperature',
            destination: '$.line1_temperature',
          },
          {
            sourceRef: { id: 'boiler/temperature', type: DataIdentifierReference.type.TAG, scope: 'boiler-line-2' },
            source: '$.temperature',
            destination: '$.line2_temperature',
          },
          {
            sourceRef: { id: 'boiler/pressure', type: DataIdentifierReference.type.TAG, scope: 'boiler-line-2' },
            source: '$.pressure',
            destination: '$.line2_pressure',
          },
        ],
      }

      cy.mountWithProviders(
        <CustomFormTesting
          schema={mockDataCombiningTableSchema}
          uiSchema={mockDataCombiningTableUISchema}
          formData={formData}
        />
      )

      cy.wait('@schema1')
      cy.wait('@schema2')

      // Verify all 3 source schema panels have loaded (one per unique instruction sourceRef)
      cy.getByTestId('combining-editor-sources-schemas').within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 3)
        cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'boiler-line-1')
        cy.getByTestId('topic-wrapper').eq(1).should('contain.text', 'boiler-line-2')
        cy.getByTestId('topic-wrapper').eq(2).should('contain.text', 'boiler-line-2')
      })
      // Verify all 3 destination instructions have ownership chips
      cy.getByTestId('combining-editor-destination-schema').within(() => {
        cy.getByTestId('mapping-instruction-source-owner').should('have.length', 3)
      })
    })
  })
})
