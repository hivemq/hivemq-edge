/// <reference types="cypress" />
import type { RJSFSchema, UiSchema } from '@rjsf/utils'

import RjsfMocks from '@/__test-utils__/rjsf/rjsf.mocks.tsx'
import { adapterJSFFields } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'

const MOCK_SCHEMA: RJSFSchema = {
  title: 'User Properties',
  description: 'Arbitrary properties to associate with the subscription',
  maxItems: 3,
  type: 'array',
  items: {
    type: 'object',
    properties: {
      name: {
        type: 'string',
        title: 'Property Name',
        description: 'Name of the associated property',
      },
      value: {
        type: 'string',
        title: 'Property Value',
        description: 'Value of the associated property',
      },
    },
    required: ['name', 'value'],
  },
}

const MOCK_UI_SCHEMA: UiSchema = {
  'ui:field': 'compactTable',
}

const MOCK_DATA = [
  {
    name: 'name1',
    value: '1',
  },
  {
    name: 'name2',
    value: 'value',
  },
]

describe('CompactArrayField', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render the compact table ', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(
      <RjsfMocks
        schema={MOCK_SCHEMA}
        uiSchema={MOCK_UI_SCHEMA}
        formData={MOCK_DATA}
        onSubmit={onSubmit}
        fields={adapterJSFFields}
      />
    )

    cy.get('[role="group"] h5').should('contain.text', 'User Properties')
    cy.get('[role="group"] sup').should('contain.text', 'Arbitrary properties to associate with the subscription')
    cy.get('table thead tr th').should('have.length', 3)
    cy.get('table thead tr th').eq(0).should('contain.text', 'Property Name')
    cy.get('table thead tr th').eq(1).should('contain.text', 'Property Value')
    cy.get('table thead tr th').eq(2).should('contain.text', 'Actions')

    cy.get('table tbody tr').should('have.length', 2)
    cy.get('table tbody td input').should('have.length', 4)
    cy.get('table tbody td input').eq(0).should('have.value', 'name1')
    cy.get('table tbody td input').eq(1).should('have.value', '1')
    cy.get('table tbody td input').eq(2).should('have.value', 'name2')
    cy.get('table tbody td input').eq(3).should('have.value', 'value')

    cy.get("button[type='submit']").click()
    cy.get('@onSubmit').should(
      'have.been.calledWith',
      Cypress.sinon.match({
        formData: MOCK_DATA,
      })
    )
  })

  it('should add and remove items', () => {
    cy.mountWithProviders(
      <RjsfMocks schema={MOCK_SCHEMA} uiSchema={MOCK_UI_SCHEMA} formData={MOCK_DATA} fields={adapterJSFFields} />
    )

    cy.get('table tbody tr').should('have.length', 2)
    cy.getByTestId('compact-add-item').click()
    cy.get('table tbody tr').should('have.length', 3)
    cy.get('table tbody td input')
      .eq(4)
      .should('have.value', '')
      .should('have.attr', 'required', 'required')
      .should('have.attr', 'aria-invalid', 'true')
    cy.get('[role="alert"] ul li').should('have.length', 2)

    cy.getByTestId('compact-delete-item').eq(2).click()
    cy.get('table tbody tr').should('have.length', 2)
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <RjsfMocks
        schema={MOCK_SCHEMA}
        uiSchema={MOCK_UI_SCHEMA}
        formData={MOCK_DATA}
        onSubmit={(e) => console.log(e)}
        fields={adapterJSFFields}
      />
    )

    cy.checkAccessibility(undefined, {
      rules: {
        // h5 used for sections is not in order. Not detected on other tests
        'heading-order': { enabled: false },
      },
    })
  })
})
