import type { WidgetProps } from '@rjsf/utils'

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'

import { EntityReferenceTableWidget } from './EntityReferenceTableWidget'

const MOCK_ENTITY_PROPS: WidgetProps<WidgetProps<Array<EntityReference>>> = {
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

const cy_withinActionButtonFromRow = (index: number, fn: (currentSubject: JQuery<HTMLElement>) => void) => {
  cy.get('table tbody tr')
    .eq(index)
    .within(() => {
      cy.get('td').eq(1).within(fn)
    })
}

describe('EntityReferenceTableWidget', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<EntityReferenceTableWidget {...MOCK_ENTITY_PROPS} />)

    cy.get('table').should('have.attr', 'aria-label', 'The list of data sources available for this combiner')
    cy.get('table thead tr th').should('have.length', 2)
    cy.get('table thead tr th').eq(0).should('have.text', 'Source')
    cy.get('table thead tr th').eq(1).should('have.text', 'Actions')

    cy.get('table tbody tr').should('have.length', 2)

    cy.get('nav').should('be.visible').should('have.attr', 'aria-label', 'Pagination Toolbar')
    cy.get('nav').find('[role="group"]').should('have.length', 2)
  })

  it('should render permanent ', () => {
    const v = { ...MOCK_ENTITY_PROPS }
    ;(v.value as Array<EntityReference>).push(
      {
        type: EntityType.PULSE_AGENT,
        id: 'my-pulse',
      },
      {
        type: EntityType.EDGE_BROKER,
        id: 'my-pulse',
      }
    )
    cy.mountWithProviders(<EntityReferenceTableWidget {...v} />)

    cy.get('table thead tr th').should('have.length', 2)
    cy.get('table tbody tr').should('have.length', 4)

    cy_withinActionButtonFromRow(0, () => {
      cy.get('button').should('have.attr', 'aria-label', 'Delete the source')
    })

    cy_withinActionButtonFromRow(2, () => {
      cy.get('button').should('not.exist')
    })

    cy_withinActionButtonFromRow(3, () => {
      cy.get('button').should('not.exist')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<EntityReferenceTableWidget {...MOCK_ENTITY_PROPS} />)
    cy.checkAccessibility()
  })
})
