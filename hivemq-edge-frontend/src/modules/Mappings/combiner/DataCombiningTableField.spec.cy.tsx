import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import { mockAdapter_OPCUA, mockProtocolAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { combinerMappingJsonSchema } from '@/api/schemas/combiner-mapping.json-schema'
import { formatTopicString } from '@/components/MQTT/topic-utils'

import { DataCombiningTableField } from './DataCombiningTableField'

const mockDataCombiningTableSchema = { ...combinerMappingJsonSchema, $ref: '#/definitions/DataCombiningList' }

const mockDataCombiningTableUISchema = {
  items: {
    'ui:field': DataCombiningTableField,
  },
}

const mockPrimary: DataCombining = {
  ...mockCombinerMapping,
  sources: {
    ...mockCombinerMapping.sources,
    primary: {
      id: 'my/tag/t3',
      type: DataIdentifierReference.type.TAG,
    },
  },
}

describe('DataCombiningTableField', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the base table', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={{
          items: [],
        }}
      />
    )

    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('thead tr th').eq(0).should('have.text', 'destination')
      cy.get('thead tr th').eq(1).should('have.text', 'sources')
      cy.get('thead tr th').eq(2).should('have.text', 'Actions')

      cy.get('tbody tr').should('have.length', 1)
      cy.get('tfoot tr td').should('have.length', 3)
      cy.get('tfoot tr td')
        .eq(2)
        .within(() => {
          cy.get('button').should('have.attr', 'aria-label', 'Add a new mapping')
        })
    })

    cy.get('form nav').should('exist')
  })

  it('should render an empty table', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={{
          items: [],
        }}
      />
    )

    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('tbody tr td')
        .should('have.length', 1)
        .should('have.attr', 'colspan', 3)
        .should('have.text', 'No data received yet.')
    })
    cy.get('form nav').should('exist')
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter_OPCUA] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter_OPCUA] })

    const onChange = cy.stub()
    const onSubmit = cy.stub()
    const onError = cy.stub()

    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={{
          items: [mockPrimary],
        }}
        onChange={onChange}
        onSubmit={onSubmit}
        onError={onError}
      />
    )

    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('tbody tr td').should('have.length', 3)
      cy.get('tbody tr td')
        .eq(0)
        .within(() => {
          cy.getByTestId('topic-wrapper').should('have.text', formatTopicString('my/topic'))
          cy.getByTestId('topic-wrapper').find('svg').should('have.attr', 'aria-label', 'Topic')
        })
    })

    cy.get('tbody tr td')
      .eq(1)
      .within(() => {
        cy.getByTestId('topic-wrapper').should('have.length', 3)
        cy.getByTestId('topic-wrapper').eq(0).should('have.text', formatTopicString('my/tag/t1'))
        cy.getByTestId('topic-wrapper').eq(0).find('svg').should('have.attr', 'aria-label', 'Tag')

        cy.getByTestId('topic-wrapper').eq(1).should('have.text', formatTopicString('my/tag/t3'))
        cy.getByTestId('topic-wrapper').eq(1).find('svg').should('have.attr', 'aria-label', 'Tag')
        cy.getByTestId('primary-wrapper').should('have.text', formatTopicString('my/tag/t3'))
        cy.getByTestId('primary-wrapper').find('svg').should('have.attr', 'aria-label', 'Primary key')

        cy.getByTestId('topic-wrapper').eq(2).should('have.text', formatTopicString('my/topic/+/temp'))
        cy.getByTestId('topic-wrapper').eq(2).find('svg').should('have.attr', 'aria-label', 'Topic Filter')
      })

    cy.get('tbody tr td')
      .eq(2)
      .within(() => {
        cy.get('button').eq(0).as('actEdit').should('have.attr', 'aria-label', 'Edit the mapping')
        cy.get('button').eq(1).as('actDelete').should('have.attr', 'aria-label', 'Delete the mapping')
      })

    cy.get('[role="dialog"]').should('not.exist')
    cy.get('@actEdit').click()

    // cy.wait(300)
    cy.get('[role="dialog"]').should('be.visible')

    cy.get('[role="dialog"]').within(() => {
      cy.get('[role="group"]:has(> label#root_id-label) > input').should(
        'have.value',
        '58677276-fc48-4a9a-880c-41c755f5063b'
      )

      cy.getByAriaLabel('Close').click()
    })
    cy.get('[role="dialog"]').should('not.exist')

    cy.get('@actDelete').click()
    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('tbody tr td')
        .should('have.length', 1)
        .should('have.attr', 'colspan', 3)
        .should('have.text', 'No data received yet.')

      cy.get('tfoot tr td')
        .eq(2)
        .within(() => {
          cy.get('button').should('have.attr', 'aria-label', 'Add a new mapping')
          cy.get('button').click()
        })

      cy.get('tbody tr td').eq(0).should('have.text', '< unset >')
      cy.get('tbody tr td').eq(1).should('have.text', '< unset >')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockDataCombiningTableSchema}
        uiSchema={mockDataCombiningTableUISchema}
        formData={{ items: [mockPrimary] }}
      />
    )
    cy.checkAccessibility()
  })
})
