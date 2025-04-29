/// <reference types="cypress" />

import {
  SchemaNameCreatableSelect,
  ScriptNameCreatableSelect,
} from '@datahub/components/forms/ResourceNameCreatableSelect.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'

// @ts-ignore No need for the whole props for testing
const MOCK_RESOURCE_NAME_PROPS: WidgetProps = {
  id: 'resource',
  label: 'Select a resource',
  name: 'transition-select',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
}

describe('SchemaNameCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] }).as('getSchemas')
  })

  it('should properly render the SchemaNameCreatableSelect', () => {
    cy.mountWithProviders(<SchemaNameCreatableSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').should('have.text', 'Select a resource')
    cy.get('#resource-label + div').should('have.text', 'Select...')
    cy.get('#resource-label').click()
    cy.get('#resource').type('my')

    cy.get('#resource-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 2)
    cy.get('@optionList').eq(0).should('contain.text', 'my-schema-id')
    cy.get('@optionList').eq(1).should('contain.text', 'Create a new Schema "my"')
    cy.get('@optionList').eq(0).click()
    // cy.get('#resource-label + div').should('have.text', 'my-schema-id')

    cy.get('#resource-label').click()
    cy.get('#resource').type('my-new-schema')
    cy.get('@optionList').eq(0).click()
    cy.get('#resource').type('my')
    cy.get('@optionList').should('have.length', 3)
    cy.get('@optionList').eq(1).should('contain.text', 'my-new-schema (Draft)')
    cy.get('@optionList').eq(2).should('contain.text', 'Create a new Schema "my"')
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(<SchemaNameCreatableSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').click()
    cy.get('#resource').type('my')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})

describe('ScriptNameCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] }).as('getSchemas')
  })

  it('should render the ScriptNameCreatableSelect', () => {
    cy.injectAxe()

    cy.mountWithProviders(<ScriptNameCreatableSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').should('have.text', 'Select a resource')
    cy.get('#resource-label + div').should('have.text', 'Select...')
    cy.get('#resource-label').click()
    cy.get('#resource').type('my')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
        'scrollable-region-focusable': { enabled: false },
      },
    })
  })
})
