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
      cy.intercept('/api/v1/data-hub/scripts*', {
        items: [mockScript, { ...mockScript, version: 2 }],
      }).as('getScripts')

      cy.mountWithProviders(<ScriptTable />)
      cy.wait('@getScripts')

      // Expand to show versions
      cy.getByAriaLabel('Show the versions').click()
      cy.get('tbody tr').should('have.length', 3)

      // Edit button should exist on individual versions (rows 1 and 2)
      cy.get('tbody tr').eq(1).find('[data-testid="list-action-edit"]').should('exist')
      cy.get('tbody tr').eq(2).find('[data-testid="list-action-edit"]').should('exist')
    })

    it.skip('should open ScriptEditor in modify mode when Edit is clicked', () => {
      // Test: Intercept scripts with multiple versions
      // Test: Expand versions
      // Test: Click Edit on a specific version
      // Test: Verify ScriptEditor opens with "Create New Script Version" title
      // Test: Verify name field is readonly
      // Test: Verify version shows "MODIFIED"
    })

    it.skip('should close ScriptEditor when close button is clicked', () => {
      // Test: Open editor via Create New
      // Test: Click close button on drawer
      // Test: Verify drawer closes
      // Test: Verify Create New button still visible
    })

    it.skip('should refresh table after successful script creation', () => {
      // Test: Open editor
      // Test: Fill form with new script
      // Test: Intercept POST /api/v1/data-hub/scripts with success
      // Test: Intercept GET /api/v1/data-hub/scripts with updated list
      // Test: Click Save
      // Test: Verify drawer closes
      // Test: Verify table refreshes with new script
    })
  })
})
