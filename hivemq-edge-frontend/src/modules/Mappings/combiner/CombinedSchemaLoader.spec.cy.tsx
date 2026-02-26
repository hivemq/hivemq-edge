import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import type { CombinerContext } from '@/modules/Mappings/types'

import { CombinedSchemaLoader } from './CombinedSchemaLoader'

const writingSchemaUrl = (adapterId: string, tagName: string) =>
  `/api/v1/management/protocol-adapters/writing-schema/${adapterId}/${encodeURIComponent(tagName)}`

const mockFormData: DataCombining = {
  ...mockCombinerMapping,
  sources: {
    primary: { id: '', type: DataIdentifierReference.type.TAG },
    tags: ['test'],
    topicFilters: ['truc'],
  },
}

const mockFormContext: CombinerContext = {
  queries: [],
  entities: [],
}

describe('CombinedSchemaLoader', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render warning', () => {
    cy.mountWithProviders(<CombinedSchemaLoader />)

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'There are no schemas available yet')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<CombinedSchemaLoader formData={mockFormData} formContext={mockFormContext} />)

    // TODO[NVL] Need to build a wrapper and generate the mocks
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'There are no schemas available yet')
  })

  describe('error fallback heading — ownership display', () => {
    it('should show ownership string in error heading when scope is set', () => {
      cy.intercept('GET', writingSchemaUrl('opcua-adapter', 'temperature'), {
        statusCode: 404,
        body: { message: 'Adapter not found' },
      }).as('schemaFail')

      const context: CombinerContext = {
        selectedSources: {
          tags: [{ id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'opcua-adapter' }],
          topicFilters: [],
        },
      }

      cy.mountWithProviders(<CombinedSchemaLoader formContext={context} />)
      cy.wait('@schemaFail')

      cy.getByTestId('topic-wrapper').should('contain.text', 'opcua-adapter :: temperature')
    })

    it('should show plain tag name in error heading when scope is null', () => {
      cy.intercept('GET', writingSchemaUrl('', 'temperature'), { statusCode: 404 }).as('schemaFail')

      const context: CombinerContext = {
        selectedSources: {
          tags: [{ id: 'temperature', type: DataIdentifierReference.type.TAG, scope: null }],
          topicFilters: [],
        },
      }

      cy.mountWithProviders(<CombinedSchemaLoader formContext={context} />)

      cy.getByTestId('topic-wrapper').should('contain.text', 'temperature')
      cy.getByTestId('topic-wrapper').should('not.contain.text', '::')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CombinedSchemaLoader />)

    cy.checkAccessibility()
  })
})
