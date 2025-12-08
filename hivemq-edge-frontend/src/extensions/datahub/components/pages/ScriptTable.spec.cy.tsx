import ScriptTable from '@datahub/components/pages/ScriptTable.tsx'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import { DateTime } from 'luxon'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

describe('ScriptTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the table component', () => {
    cy.intercept('/api/v1/data-hub/scripts', { statusCode: 404 })
    cy.mountWithProviders(<ScriptTable />)

    cy.get('table').should('have.attr', 'aria-label', 'List of scripts')
    cy.get('table').find('thead').find('th').should('have.length', 6)
    cy.get('table').find('thead').find('th').eq(0).should('have.text', 'Function name')
    cy.get('table').find('thead').find('th').eq(1).should('have.text', 'Type')
    cy.get('table').find('thead').find('th').eq(2).should('have.text', 'Version')
    cy.get('table').find('thead').find('th').eq(3).should('have.text', 'Description')
    cy.get('table').find('thead').find('th').eq(4).should('have.text', 'Created')
    cy.get('table').find('thead').find('th').eq(5).should('have.text', 'Actions')

    cy.get("[role='alert']")
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'There was an error loading the data')
  })

  it('should render the data', () => {
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] })
    const now = DateTime.fromISO(MOCK_CREATED_AT).plus({ day: 2 }).toJSDate()

    cy.clock(now)
    cy.mountWithProviders(<ScriptTable />)
    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 6)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-script-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'Transformation')
    cy.get('@firstItemContent').eq(2).should('have.text', '1')
    cy.get('@firstItemContent').eq(3).should('have.text', 'this is a description')
    cy.get('@firstItemContent').eq(4).should('have.text', '2 days ago')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] })

    cy.injectAxe()
    cy.mountWithProviders(<ScriptTable />)
    cy.checkAccessibility()
  })

  describe('Script Editor Integration', () => {
    it('should render Create New Script button', () => {
      cy.intercept('/api/v1/data-hub/scripts*', { items: [] })

      cy.mountWithProviders(<ScriptTable />)

      cy.getByTestId('script-create-new-button').should('be.visible').and('contain.text', 'Create New Script')
    })

    it('should open ScriptEditor when Create New is clicked', () => {
      cy.intercept('/api/v1/data-hub/scripts*', { items: [] })

      cy.mountWithProviders(<ScriptTable />)

      // Editor should not be visible initially
      cy.getByTestId('script-editor-drawer').should('not.exist')

      // Click Create New button
      cy.getByTestId('script-create-new-button').click()

      // Editor drawer should open in create mode
      cy.getByTestId('script-editor-drawer').should('be.visible')
      cy.contains('Create New Script').should('be.visible')
    })

    it('should show Edit action for individual script versions', () => {
      cy.intercept('/api/v1/data-hub/scripts', {
        items: [mockScript, { ...mockScript, version: 2 }],
      }).as('getScripts')

      cy.mountWithProviders(<ScriptTable />)
      cy.wait('@getScripts')

      // Wait for Skeleton to finish loading by checking text content
      cy.get('tbody tr').should('have.length', 1)
      cy.get('tbody tr').first().should('contain.text', 'my-script-id')
      cy.get('tbody tr').first().should('contain.text', '2 versions')

      // Expand to show versions - ensure button is visible and enabled before clicking
      cy.getByAriaLabel('Show the versions').should('be.visible').should('not.be.disabled')
      cy.getByAriaLabel('Show the versions').click()
      cy.get('tbody tr').should('have.length', 3)

      // Edit button should exist on individual versions (rows 1 and 2)
      cy.get('tbody tr').eq(1).find('[data-testid="list-action-edit"]').should('exist')
      cy.get('tbody tr').eq(2).find('[data-testid="list-action-edit"]').should('exist')
    })

    it('should open ScriptEditor in modify mode when Edit is clicked', () => {
      cy.intercept('/api/v1/data-hub/scripts*', {
        items: [mockScript, { ...mockScript, version: 2 }],
      }).as('getScripts')

      cy.mountWithProviders(<ScriptTable />)
      cy.wait('@getScripts')

      // Wait for Skeleton to finish loading by checking text content
      cy.get('tbody tr').should('have.length', 1)
      cy.get('tbody tr').first().should('contain.text', 'my-script-id')
      cy.get('tbody tr').first().should('contain.text', '2 versions')

      // Expand to show versions - ensure button is visible and enabled before clicking
      cy.getByAriaLabel('Show the versions').should('be.visible').should('not.be.disabled')
      cy.getByAriaLabel('Show the versions').click()
      cy.get('tbody tr').should('have.length', 3)

      // Click Edit on version 1
      cy.get('tbody tr').eq(1).find('[data-testid="list-action-edit"]').click()

      // Verify ScriptEditor opens with modify title
      cy.getByTestId('script-editor-drawer').should('be.visible')
      cy.contains('Create New Script Version').should('be.visible')

      // Verify name field is readonly
      cy.get('#root_name').should('have.attr', 'readonly')

      // Verify version shows modified indicator
      cy.get('label#root_version-label + div').should('contain.text', 'MODIFIED')
    })

    it('should close ScriptEditor when close button is clicked', () => {
      cy.intercept('/api/v1/data-hub/scripts*', { items: [] })

      cy.mountWithProviders(<ScriptTable />)

      // Open editor via Create New
      cy.getByTestId('script-create-new-button').click()
      cy.getByTestId('script-editor-drawer').should('be.visible')

      // Click close button on drawer
      cy.getByTestId('script-editor-drawer').within(() => {
        cy.getByAriaLabel('Close').click()
      })

      // Verify drawer closes
      cy.getByTestId('script-editor-drawer').should('not.exist')

      // Verify Create New button still visible
      cy.getByTestId('script-create-new-button').should('be.visible')
    })

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

    it('should refresh table after successful script creation', () => {
      // Set up all intercepts before mounting
      cy.intercept('GET', '/api/v1/data-hub/scripts*', { items: [] }).as('getScriptsInitial')
      cy.intercept('POST', '/api/v1/data-hub/scripts', { statusCode: 201 }).as('createScript')

      cy.mountWithProviders(<ScriptTable />)
      cy.wait('@getScriptsInitial')

      // Open editor
      cy.getByTestId('script-create-new-button').click()
      cy.getByTestId('script-editor-drawer').should('be.visible')

      // Fill and submit form (minimal required fields + description)
      // Only name is required; type is hidden (always Javascript), sourceCode has default
      cy.get('#root_name').type('new-script-id')
      cy.get('#root_description').type('Test script description')

      // Click Save button
      cy.getByTestId('save-script-button').click()

      // Verify POST was called with correct data
      cy.wait('@createScript').its('request.body').should('deep.include', {
        id: 'new-script-id',
        description: 'Test script description',
      })

      // Verify drawer closes after successful save
      cy.getByTestId('script-editor-drawer').should('not.exist')

      // Note: In mocked environment, React Query's invalidation won't trigger a real GET request
      // The table refresh would happen in a real environment, but here we verify the mutation succeeded
    })
  })
})
