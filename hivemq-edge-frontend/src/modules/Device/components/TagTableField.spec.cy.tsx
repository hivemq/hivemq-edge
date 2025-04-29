import type { RJSFSchema } from '@rjsf/utils'

import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { tagListJsonSchema } from '@/api/schemas'
import { MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA, MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { formatTopicString } from '@/components/MQTT/topic-utils'
import type { DeviceTagListContext } from '../types'

import { TagTableField } from './TagTableField'

const mockTagListUISchema = {
  items: {
    'ui:field': TagTableField,
  },
}

const mockTagListSchema = {
  ...tagListJsonSchema,
  definitions: {
    TagSchema: MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA.configSchema as RJSFSchema,
  },
}

describe('TagTableField', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the base table', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockTagListSchema}
        uiSchema={mockTagListUISchema}
        formData={{
          items: [],
        }}
      />
    )

    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('thead tr th').eq(0).should('have.text', 'Tag')
      cy.get('thead tr th').eq(1).should('have.text', 'Description')
      cy.get('thead tr th').eq(2).should('have.text', 'Actions')

      cy.get('tbody tr').should('have.length', 1)
      cy.get('tfoot tr td').should('have.length', 3)
      cy.get('tfoot tr td')
        .eq(2)
        .within(() => {
          cy.get('button').should('have.attr', 'aria-label', 'Add a new tag')
        })
    })

    cy.get('form nav').should('exist')
  })

  it('should render an empty table', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockTagListSchema}
        uiSchema={mockTagListUISchema}
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
        .should('have.text', 'There is no tag created yet')
    })
    cy.get('form nav').should('exist')
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    const onSubmit = cy.stub().as('onSubmit')
    const onError = cy.stub().as('onError')

    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockTagListSchema}
        uiSchema={mockTagListUISchema}
        formData={{
          items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
        }}
        formContext={{ adapterId: 'opcua-1', capabilities: ['READ', 'WRITE'] } as DeviceTagListContext}
        onChange={onChange}
        onSubmit={onSubmit}
        onError={onError}
      />
    )

    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('tbody tr td').should('have.length', 6)
      cy.get('tbody tr td')
        .eq(0)
        .within(() => {
          cy.getByTestId('topic-wrapper').should('have.text', formatTopicString('opcua-1/power/off'))
          cy.getByTestId('topic-wrapper').find('svg').should('have.attr', 'aria-label', 'Tag')
        })
    })

    cy.get('tbody tr td').eq(1).should('contain.text', 'This is a very long description')

    cy.get('tbody tr td')
      .eq(2)
      .within(() => {
        cy.get('button').eq(0).as('actEdit').should('have.attr', 'aria-label', 'Edit the tag')
        cy.get('button').eq(1).as('actDelete').should('have.attr', 'aria-label', 'Delete the tag')
        cy.get('button').eq(2).as('actSchema').should('have.attr', 'aria-label', 'View the WRITE schema')
      })

    cy.get('[role="dialog"]').should('not.exist')
    cy.get('@actSchema').click()
    cy.get('[role="dialog"]').should('be.visible')
    cy.get('[role="dialog"]').within(() => {
      cy.get('header').should('have.text', 'Manage the schema for the tag')

      cy.getByAriaLabel('Close').click()
    })

    cy.get('[role="dialog"]').should('not.exist')
    cy.get('@actEdit').click()

    // cy.wait(300)
    cy.get('[role="dialog"]').should('be.visible')

    cy.get('[role="dialog"]').within(() => {
      cy.get('[role="group"]:has(> label#root_name-label) > input').should('have.value', 'opcua-1/power/off')

      cy.getByAriaLabel('Close').click()
    })
    cy.get('[role="dialog"]').should('not.exist')

    cy.get('@actDelete').click()
    cy.get('form table').within(() => {
      cy.get('thead tr th').should('have.length', 3)
      cy.get('tbody tr td').should('have.length', 3)

      cy.get('tfoot tr td')
        .eq(2)
        .within(() => {
          cy.get('button').should('have.attr', 'aria-label', 'Add a new tag')
          cy.get('button').click()
        })

      cy.get('tbody tr ').eq(1).should('contain.text', '< unset >')
    })
  })

  it('should not show schema if not WRITE', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockTagListSchema}
        uiSchema={mockTagListUISchema}
        formData={{
          items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
        }}
        formContext={{ adapterId: 'opcua-1', capabilities: ['READ'] } as DeviceTagListContext}
        onChange={cy.stub}
        onSubmit={cy.stub}
        onError={cy.stub}
      />
    )

    cy.get('tbody tr td')
      .eq(2)
      .within(() => {
        cy.get('button').eq(0).as('actEdit').should('have.attr', 'aria-label', 'Edit the tag')
        cy.get('button').eq(1).as('actDelete').should('have.attr', 'aria-label', 'Delete the tag')
        cy.getByAriaLabel('View the WRITE schema').should('not.exist')
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockTagListSchema}
        uiSchema={mockTagListUISchema}
        formData={{
          items: MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA),
        }}
      />
    )
    cy.checkAccessibility()
  })
})
