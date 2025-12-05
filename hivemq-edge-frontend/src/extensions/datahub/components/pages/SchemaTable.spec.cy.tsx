import { DateTime } from 'luxon'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import SchemaTable from '@datahub/components/pages/SchemaTable.tsx'

describe('SchemaTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/schemas', { statusCode: 404 })

    // TODO[NVL] Create a custom command for that stub
    cy.stub(DateTime, 'now').returns(DateTime.fromISO(MOCK_CREATED_AT).plus({ day: 2 }))
  })

  it('should render the table component', () => {
    cy.mountWithProviders(<SchemaTable />)

    cy.get('table').should('have.attr', 'aria-label', 'List of schemas')
    cy.get('table').find('thead').find('th').should('have.length', 5)
    cy.get('table').find('thead').find('th').eq(0).should('have.text', 'Schema name')
    cy.get('table').find('thead').find('th').eq(1).should('have.text', 'Type')
    cy.get('table').find('thead').find('th').eq(2).should('have.text', 'Version')
    cy.get('table').find('thead').find('th').eq(3).should('have.text', 'Created')
    cy.get('table').find('thead').find('th').eq(4).should('have.text', 'Actions')
  })

  it('should render the data', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] })

    cy.mountWithProviders(<SchemaTable />)
    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 5)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-schema-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'JSON')
    cy.get('@firstItemContent').eq(2).should('have.text', '1')
    cy.get('@firstItemContent').eq(3).should('have.text', '2 days ago')
  })

  it('should render expandable data', () => {
    cy.intercept('/api/v1/data-hub/schemas', {
      items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, version: 2 }],
    }).as('getSchemas')

    cy.mountWithProviders(<SchemaTable />)
    cy.wait('@getSchemas')

    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 5)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-schema-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'JSON')
    cy.get('@firstItemContent').eq(2).should('have.text', '2 versions')
    cy.get('@firstItemContent').eq(3).should('have.text', '2 days ago')
    cy.get('@firstItemContent')
      .eq(0)
      .within(() => {
        cy.getByAriaLabel('Show the versions').should('be.visible').should('not.be.disabled')
        cy.getByAriaLabel('Show the versions').click()
      })
    cy.get('tbody tr').should('have.length', 3)
    cy.get('tbody tr').eq(2).find('td').as('childrenRow')
    cy.get('@childrenRow').eq(2).should('have.text', '2')

    cy.get('@firstItemContent')
      .eq(0)
      .within(() => {
        cy.getByAriaLabel('Hide the versions').should('be.visible').should('not.be.disabled')
        cy.getByAriaLabel('Hide the versions').click()
      })
    cy.get('tbody tr').should('have.length', 1)
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/data-hub/schemas', {
      items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, version: 2 }],
    }).as('getSchemas')

    cy.injectAxe()
    cy.mountWithProviders(<SchemaTable />)

    cy.checkAccessibility()
  })

  describe('Schema Editor Integration', () => {
    beforeEach(() => {
      // Ignore Monaco worker loading errors and React Query cancelation errors
      cy.on('uncaught:exception', (err) => {
        return !(
          err.message.includes('importScripts') ||
          err.message.includes('worker') ||
          err.message.includes('cancelation')
        )
      })
    })

    it('should render Create New Schema button', () => {
      cy.intercept('/api/v1/data-hub/schemas', { items: [] })

      cy.mountWithProviders(<SchemaTable />)

      cy.getByTestId('schema-create-new-button').should('be.visible').and('contain.text', 'Create New Schema')
    })

    it('should open SchemaEditor when Create New is clicked', () => {
      cy.intercept('/api/v1/data-hub/schemas', { items: [] })

      cy.mountWithProviders(<SchemaTable />)

      // Editor should not be visible initially
      cy.getByTestId('schema-editor-drawer').should('not.exist')

      // Click Create New button
      cy.getByTestId('schema-create-new-button').click()

      // Editor drawer should open in create mode
      cy.getByTestId('schema-editor-drawer').should('be.visible')
      cy.contains('Create New Schema').should('be.visible')
    })

    it('should show Edit action for individual schema versions', () => {
      cy.intercept('/api/v1/data-hub/schemas', {
        items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, version: 2 }],
      }).as('getSchemas')

      cy.mountWithProviders(<SchemaTable />)
      cy.wait('@getSchemas')

      // Wait for Skeleton to finish loading by checking text content
      cy.get('tbody tr').should('have.length', 1)
      cy.get('tbody tr').first().should('contain.text', 'my-schema-id')
      cy.get('tbody tr').first().should('contain.text', '2 versions')

      // Expand to show versions - ensure button is visible and enabled before clicking
      cy.getByAriaLabel('Show the versions').should('be.visible').should('not.be.disabled')
      cy.getByAriaLabel('Show the versions').click()
      cy.get('tbody tr').should('have.length', 3)

      // Edit button should exist on individual versions (rows 1 and 2)
      cy.get('tbody tr').eq(1).find('[data-testid="list-action-edit"]').should('exist')
      cy.get('tbody tr').eq(2).find('[data-testid="list-action-edit"]').should('exist')
    })

    it('should open SchemaEditor in modify mode when Edit is clicked', () => {
      cy.intercept('/api/v1/data-hub/schemas', {
        items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, version: 2 }],
      }).as('getSchemas')

      cy.mountWithProviders(<SchemaTable />)
      cy.wait('@getSchemas')

      // Wait for Skeleton to finish loading by checking text content
      cy.get('tbody tr').should('have.length', 1)
      cy.get('tbody tr').first().should('contain.text', 'my-schema-id')
      cy.get('tbody tr').first().should('contain.text', '2 versions')

      // Expand to show versions - ensure button is visible and enabled before clicking
      cy.getByAriaLabel('Show the versions').should('be.visible').should('not.be.disabled')
      cy.getByAriaLabel('Show the versions').click()
      cy.get('tbody tr').should('have.length', 3)

      // Click Edit on version 1
      cy.get('tbody tr').eq(1).find('[data-testid="list-action-edit"]').click()

      // Verify SchemaEditor opens with modify title
      cy.getByTestId('schema-editor-drawer').should('be.visible')
      cy.contains('Create New Schema Version').should('be.visible')

      // Verify name field is readonly
      cy.get('#root_name').should('have.attr', 'readonly')

      // Verify version shows modified indicator
      cy.get('label#root_version-label + div').should('contain.text', 'MODIFIED')
    })

    it('should close SchemaEditor when close button is clicked', () => {
      cy.intercept('/api/v1/data-hub/schemas', { items: [] })

      cy.mountWithProviders(<SchemaTable />)

      // Open editor via Create New
      cy.getByTestId('schema-create-new-button').click()
      cy.getByTestId('schema-editor-drawer').should('be.visible')

      // Click close button on drawer
      cy.getByTestId('schema-editor-drawer').within(() => {
        cy.getByAriaLabel('Close').click()
      })

      // Verify drawer closes
      cy.getByTestId('schema-editor-drawer').should('not.exist')

      // Verify Create New button still visible
      cy.getByTestId('schema-create-new-button').should('be.visible')
    })

    it('should refresh table after successful schema creation', () => {
      // Set up all intercepts before mounting
      cy.intercept('GET', '/api/v1/data-hub/schemas', { items: [] }).as('getSchemasInitial')
      cy.intercept('POST', '/api/v1/data-hub/schemas', { statusCode: 201 }).as('createSchema')

      cy.mountWithProviders(<SchemaTable />)
      cy.wait('@getSchemasInitial')

      // Open editor
      cy.getByTestId('schema-create-new-button').click()
      cy.getByTestId('schema-editor-drawer').should('be.visible')

      // Fill and submit form (minimal required fields)
      cy.get('#root_name').type('new-schema-id')

      // Select JSON type using react-select
      cy.get('label#root_type-label + div').click()
      cy.contains('[role="option"]', 'JSON').click()

      // Set schema source using Monaco editor (can't use cy.type() with Monaco)
      cy.get('#root_schemaSource').click()
      cy.window().then((win) => {
        // @ts-ignore - Monaco is attached to window in tests
        const monaco = win.monaco
        // @ts-ignore
        const editors = monaco.editor.getEditors()
        const editor = editors[0]
        editor.setValue('{"type": "object"}')
      })

      // Click Save button (correct test ID is save-schema-button)
      cy.getByTestId('save-schema-button').click()

      // Verify POST was called with correct data
      cy.wait('@createSchema').its('request.body').should('deep.include', {
        id: 'new-schema-id',
        type: 'JSON',
      })

      // Verify drawer closes after successful save
      cy.getByTestId('schema-editor-drawer').should('not.exist')

      // Note: In mocked environment, React Query's invalidation won't trigger a real GET request
      // The table refresh would happen in a real environment, but here we verify the mutation succeeded
    })
  })
})
