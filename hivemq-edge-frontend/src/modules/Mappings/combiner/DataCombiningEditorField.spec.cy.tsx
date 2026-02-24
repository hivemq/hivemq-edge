/// <reference types="cypress" />

import type { UiSchema } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import { mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import { combinerMappingJsonSchema } from '@/api/schemas/combiner-mapping.json-schema'
import type { DataCombining, DomainTag, DomainTagList } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import { mockAdapter_OPCUA, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import type { CombinerContext } from '@/modules/Mappings/types'

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
      cy.get('[role="alert"]').should('have.text', 'There are no schemas available yet')
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
      cy.get('[role="group"] label + div').should('have.text', 'my/tag/t1')
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
})
