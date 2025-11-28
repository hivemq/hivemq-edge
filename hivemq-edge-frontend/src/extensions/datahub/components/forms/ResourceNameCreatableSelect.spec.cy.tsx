/// <reference types="cypress" />

import {
  SchemaNameCreatableSelect,
  ScriptNameCreatableSelect,
  SchemaNameSelect,
  ScriptNameSelect,
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

describe('SchemaNameSelect (select-only mode)', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] }).as('getSchemas')
  })

  // ✅ ACTIVE - Verify no creation allowed
  it('should NOT allow creating new schemas', () => {
    cy.mountWithProviders(<SchemaNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').should('have.text', 'Select a resource')
    cy.get('#resource-label').click()
    cy.get('#resource').type('my-new-schema')

    // Should only show existing schema, NO "Create new" option
    cy.get('#resource-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 0) // No matches, no create option

    // Search for existing schema
    cy.get('#resource').clear()
    cy.get('#resource').type('my-schema')
    cy.get('@optionList').should('have.length', 1)
    cy.get('@optionList').eq(0).should('contain.text', 'my-schema-id')
    // Verify no "Create a new Schema" option exists
    cy.get('@optionList').should('not.contain.text', 'Create a new Schema')
  })

  // ✅ ACTIVE - Accessibility check
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

  // ⏭️ SKIPPED - Will activate during Phase 4
  it.skip('should show placeholder text', () => {
    // Test: Mount component
    // Test: Verify placeholder shows "Select a Schema"
  })

  it.skip('should allow selecting existing schemas', () => {
    // Test: Mount with multiple schemas
    // Test: Click dropdown, select a schema
    // Test: Verify onChange called with schema name
  })

  it.skip('should filter schemas by search text', () => {
    // Test: Mount with multiple schemas
    // Test: Type partial name
    // Test: Verify only matching schemas shown
  })
})

describe('ScriptNameSelect (select-only mode)', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/data-hub/scripts*', { items: [mockScript] }).as('getScripts')
  })

  // ✅ ACTIVE - Verify no creation allowed
  it('should NOT allow creating new scripts', () => {
    cy.mountWithProviders(<ScriptNameSelect {...MOCK_RESOURCE_NAME_PROPS} />)
    cy.get('#resource-label').click()
    cy.get('#resource').type('my-new-script')

    // Should only show existing scripts, NO "Create new" option
    cy.get('#resource-label + div').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 0) // No matches, no create option

    // Search for existing script
    cy.get('#resource').clear()
    cy.get('#resource').type('my-script')
    cy.get('@optionList').should('have.length', 1)
    cy.get('@optionList').eq(0).should('contain.text', 'my-script-id')
    // Verify no "Create a new Function" option exists
    cy.get('@optionList').should('not.contain.text', 'Create a new Function')
  })

  // ✅ ACTIVE - Accessibility check
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

  // ⏭️ SKIPPED - Will activate during Phase 4
  it.skip('should show placeholder text', () => {
    // Test: Mount component
    // Test: Verify placeholder shows "Select a Function"
  })

  it.skip('should allow selecting existing scripts', () => {
    // Test: Mount with multiple scripts
    // Test: Click dropdown, select a script
    // Test: Verify onChange called with script name
  })

  it.skip('should filter scripts by search text', () => {
    // Test: Mount with multiple scripts
    // Test: Type partial name
    // Test: Verify only matching scripts shown
  })
})
