/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'
import type { RJSFSchema } from '@rjsf/utils/src/types'
import type { EntityReference } from '@/api/__generated__'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { EntityReferenceTableWidget } from './EntityReferenceTableWidget'

const MOCK_ENTITY_PROPS: WidgetProps<WidgetProps<Array<EntityReference>, RJSFSchema>> = {
  id: 'root_sources_items',
  label: 'items',
  name: 'items',
  value: mockCombiner.sources?.items,
  schema: {
    type: 'array',
    items: {
      description: 'A reference to one of the main entities in Edge (e.g. device, adapter, edge broker, bridge host)',
      properties: {
        type: {
          enum: ['ADAPTER', 'BRIDGE', 'EDGE_BROKER'],
        },
        id: {
          type: 'string',
          description: 'The id of the entity being references in the combiner',
        },
      },
    },
  },
  options: {},
  // @ts-ignore
  registry: {},
  onChange: () => undefined,
}

describe('EntityReferenceTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { statusCode: 404 })
    cy.intercept('/api/v1/management/bridges/*', { statusCode: 404 })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<EntityReferenceTableWidget {...MOCK_ENTITY_PROPS} />)

    cy.get('table').should('have.attr', 'aria-label', 'The list of data sources available for this combiner')
    cy.get('table thead tr th').should('have.length', 2)
    cy.get('table thead tr th').eq(0).should('have.text', 'Source')
    cy.get('table thead tr th').eq(1).should('have.text', 'Actions')

    cy.get('table tbody tr').should('have.length', 2)

    cy.get('nav').should('be.visible').should('have.attr', 'aria-label', 'Pagination')
    cy.get('nav').find('[role="group"]').should('have.length', 2)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<EntityReferenceTableWidget {...MOCK_ENTITY_PROPS} />)
    cy.checkAccessibility()
  })
})
