/// <reference types="cypress" />

import {
  SchemaNameCreatableSelect,
  ScriptNameCreatableSelect,
  SchemaNameSelect,
  ScriptNameSelect,
} from '@datahub/components/forms/ResourceNameCreatableSelect.tsx'
import { MOCK_SCHEMA_ID, mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
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
    cy.get('#resource-label + div').should('have.text', 'Select a Schema')
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
    cy.get('#resource-label + div').should('have.text', 'Select a JS Function')
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

describe('SchemaNameSelect (select-only mode)', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] }).as('getSchemas')
  })

  it('should NOT allow creating new schemas', () => {
    cy.mountWithProviders(<SchemaNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').should('have.text', 'Select a resource')
    cy.get('#resource-label').click()
    cy.get('#resource').type('my-new-schema')

    // Should only show existing schema, NO "Create new" option
    cy.get('#resource-label + div').find('[role="option"]').should('have.length', 0) // No matches, no create option

    // Search for existing schema
    cy.get('#resource').clear()
    cy.get('#resource').type('my-schema')

    // Re-query after typing new text
    cy.get('#resource-label + div').find('[role="option"]').should('have.length', 1)
    cy.get('#resource-label + div').find('[role="option"]').eq(0).should('contain.text', 'my-schema-id')
    cy.get('#resource-label + div').find('[role="option"]').should('not.contain.text', 'Create a new Schema')
  })

  it('should be accessible in select-only mode', () => {
    cy.injectAxe()

    cy.mountWithProviders(<SchemaNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').click()
    cy.get('#resource').type('my')

    cy.checkAccessibility(undefined, {
      rules: {
        'aria-input-field-name': { enabled: false },
      },
    })
  })

  it('should show placeholder text', () => {
    cy.mountWithProviders(<SchemaNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)

    // Verify placeholder shows resource-specific text
    cy.get('#resource-label + div').should('contain.text', 'Select a Schema')
  })

  it('should allow selecting existing schemas', () => {
    const mockSchemas = [
      mockSchemaTempHumidity,
      { ...mockSchemaTempHumidity, id: 'schema-2', version: 1 },
      { ...mockSchemaTempHumidity, id: 'schema-3', version: 1 },
    ]

    cy.intercept('/api/v1/data-hub/schemas', { items: mockSchemas }).as('getSchemas')

    const onChangeSpy = cy.spy().as('onChangeSpy')
    cy.mountWithProviders(<SchemaNameSelect {...MOCK_RESOURCE_NAME_PROPS} onChange={onChangeSpy} />)

    // Click and type to open dropdown and show options
    cy.get('#resource-label').click()
    cy.get('#resource').type('schema')

    // Verify schemas are listed
    cy.get('#resource-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length.at.least', 3)

    // Select first schema
    cy.get('@optionList').eq(0).click()

    // Verify onChange was called
    cy.get('@onChangeSpy').should('have.been.called')
  })

  it('should filter schemas by search text', () => {
    const mockSchemas = [
      mockSchemaTempHumidity,
      { ...mockSchemaTempHumidity, id: 'temperature-sensor', version: 1 },
      { ...mockSchemaTempHumidity, id: 'humidity-sensor', version: 1 },
    ]

    cy.intercept('/api/v1/data-hub/schemas', { items: mockSchemas }).as('getSchemas')

    cy.mountWithProviders(<SchemaNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)

    // Open dropdown and type to filter
    cy.get('#resource-label').click()
    cy.get('#resource').type('temperature')

    // Verify filtered results - re-query to get filtered options
    cy.get('#resource-label + div').find('[role="option"]').should('have.length', 3)
    cy.get('#resource-label + div').find('[role="option"]').eq(0).should('contain.text', MOCK_SCHEMA_ID)
    cy.get('#resource-label + div').find('[role="option"]').eq(1).should('contain.text', 'temperature-sensor')
    cy.get('#resource-label + div').find('[role="option"]').eq(2).should('contain.text', 'humidity-sensor')
  })
})

describe('ScriptNameSelect (select-only mode)', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/data-hub/scripts*', { items: [mockScript] }).as('getScripts')
  })

  it('should NOT allow creating new scripts', () => {
    cy.mountWithProviders(<ScriptNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').click()
    cy.get('#resource').type('my-new-script')

    // Should only show existing scripts, NO "Create new" option
    cy.get('#resource-label + div').find('[role="option"]').should('have.length', 0) // No matches, no create option

    // Search for existing script
    cy.get('#resource').clear()
    cy.get('#resource').type('my-script')

    // Re-query after typing new text
    cy.get('#resource-label + div').find('[role="option"]').should('have.length', 1)
    cy.get('#resource-label + div').find('[role="option"]').eq(0).should('contain.text', 'my-script-id')
    cy.get('#resource-label + div').find('[role="option"]').should('not.contain.text', 'Create a new Function')
  })

  it('should be accessible in select-only mode', () => {
    cy.injectAxe()

    cy.mountWithProviders(<ScriptNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').click()
    cy.get('#resource').type('my')

    cy.checkAccessibility(undefined, {
      rules: {
        'aria-input-field-name': { enabled: false },
        'scrollable-region-focusable': { enabled: false },
      },
    })
  })

  it('should show placeholder text', () => {
    cy.mountWithProviders(<ScriptNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)

    // Verify placeholder shows resource-specific text
    cy.get('#resource-label + div').should('contain.text', 'Select a JS Function')
  })

  it('should allow selecting existing scripts', () => {
    const mockScripts = [
      mockScript,
      { ...mockScript, id: 'script-2', version: 1 },
      { ...mockScript, id: 'script-3', version: 1 },
    ]

    cy.intercept('/api/v1/data-hub/scripts*', { items: mockScripts }).as('getScripts')

    const onChangeSpy = cy.spy().as('onChangeSpy')
    cy.mountWithProviders(<ScriptNameSelect {...MOCK_RESOURCE_NAME_PROPS} onChange={onChangeSpy} />)

    // Click and type to open dropdown and show options
    cy.get('#resource-label').click()
    cy.get('#resource').type('script')

    // Verify scripts are listed
    cy.get('#resource-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length.at.least', 3)

    // Select first script
    cy.get('@optionList').eq(0).click()

    // Verify onChange was called
    cy.get('@onChangeSpy').should('have.been.called')
  })

  it('should filter scripts by search text', () => {
    const mockScripts = [
      mockScript,
      { ...mockScript, id: 'temperature-converter', version: 1 },
      { ...mockScript, id: 'humidity-processor', version: 1 },
    ]

    cy.intercept('/api/v1/data-hub/scripts*', { items: mockScripts }).as('getScripts')

    cy.mountWithProviders(<ScriptNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)

    // Open dropdown and type to filter
    cy.get('#resource-label').click()
    cy.get('#resource').type('temperature')

    // Verify only matching scripts shown - re-query to get filtered options
    cy.get('#resource-label + div').find('[role="option"]').should('have.length', 1)
    cy.get('#resource-label + div').find('[role="option"]').eq(0).should('contain.text', 'temperature-converter')
  })
})
