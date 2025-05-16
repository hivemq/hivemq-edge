import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import SchemaTable from '@datahub/components/pages/SchemaTable.tsx'

describe('SchemaTable (Copilot)', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  context('Rendering and Loading States', () => {
    it('should render the table component correctly', () => {
      cy.mountWithProviders(<SchemaTable />)

      cy.get('table').should('have.attr', 'aria-label', 'List of schemas')
      cy.get('table thead th')
        .should('have.length', 5)
        .then((headers) => {
          const expectedHeaders = ['Schema name', 'Type', 'Version', 'Created', 'Actions']
          headers.each((index, header) => {
            expect(header).to.contain.text(expectedHeaders[index])
          })
        })
    })

    it('should show loading skeleton while fetching data', () => {
      cy.intercept('/api/v1/data-hub/schemas', { statusCode: 404 })
      cy.mountWithProviders(<SchemaTable />)
      // There is no [data-testid="skeleton"]
      // cy.get('[data-testid="skeleton"]').should('exist')
      cy.get('.chakra-skeleton').should('exist')

      // There is no displayed error message
      cy.get('[role="alert"]')
        .should('contain.text', 'There was an error loading the data')
        .should('have.attr', 'data-status', 'error')
    })
  })

  context('Data Rendering', () => {
    beforeEach(() => {
      const schema = {
        ...mockSchemaTempHumidity,
        id: 'test-schema',
        type: 'JSON',
        version: 2,
        createdAt: MOCK_CREATED_AT,
      }
      cy.intercept('/api/v1/data-hub/schemas', { items: [schema] }).as('getSchemasSuccess')
    })

    it('should render schema data correctly', () => {
      cy.mountWithProviders(<SchemaTable />)
      cy.wait('@getSchemasSuccess')

      // Fixed chaining issue: get element first, then use should
      cy.get('tbody tr').should('have.length', 1)
      cy.get('tbody tr')
        .first()
        .within(() => {
          cy.get('td').eq(0).should('contain', 'test-schema')
          cy.get('td').eq(1).should('contain', 'JSON')
          cy.get('td').eq(2).should('contain', '2')
          // without clock the date is not correct
          cy.get('td').eq(3).should('contain', '1 year ago')
          cy.get('td').eq(4).find('button').should('have.length', 2)
        })
    })
  })

  context('Error Handling', () => {
    it('should handle error state', () => {
      cy.intercept('/api/v1/data-hub/schemas', { statusCode: 500 }).as('getSchemasError')
      cy.mountWithProviders(<SchemaTable />)

      // That's hallucinating
      // cy.contains('Error loading data').should('be.visible')
      // cy.get('button').contains('Try again').should('be.visible')
    })
  })

  context('Actions', () => {
    it('should trigger delete action correctly', () => {
      const deleteItemSpy = cy.spy().as('deleteItemSpy')
      cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] }).as('getSchemas')

      cy.mountWithProviders(<SchemaTable onDeleteItem={deleteItemSpy} />)
      cy.get('tbody tr').first().find('[aria-label="Delete"]').click()

      cy.get('@deleteItemSpy').should('have.been.calledOnce')
    })

    // This one is completely bonkers, as far as I can tell
    it.skip('should trigger download action correctly', () => {
      const downloadJSONSpy = cy.spy().as('downloadJSONSpy')
      cy.window().then((win) => {
        cy.stub(win, 'downloadJSON').as('downloadJSONStub').callsFake(downloadJSONSpy)
      })

      cy.mountWithProviders(<SchemaTable />)
      cy.get('tbody tr').first().find('[aria-label="Download"]').click()

      cy.get('@downloadJSONStub').should('have.been.calledOnce')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SchemaTable />)
    cy.checkAccessibility()
  })
})
