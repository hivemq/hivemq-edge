import SearchBar from '@/components/PaginatedTable/components/SearchBar.tsx'

describe('SearchBar', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should not render when no tools', () => {
    cy.mountWithProviders(<SearchBar columnFilters={[]} globalFilter="" />)

    cy.get('[role="toolbar"][aria-label="Search Toolbar"]').within(() => {
      cy.getByTestId('table-search-control').should('not.exist')
      cy.getByTestId('table-filters-clearAll').should('not.exist')
    })
  })

  it('should render the search bar', () => {
    const setGlobalFilter = cy.stub().as('setGlobalFilter')

    cy.mountWithProviders(
      <SearchBar enableGlobalFilter columnFilters={[]} globalFilter="text" setGlobalFilter={setGlobalFilter} />
    )

    cy.get('@setGlobalFilter').should('not.have.been.called')
    cy.get('[role="toolbar"][aria-label="Search Toolbar"]').within(() => {
      cy.getByTestId('table-search-control').within(() => {
        cy.get('input').should('have.attr', 'placeholder', 'Search for ...').should('have.value', 'text')
        cy.getByTestId('table-search-clear').should('have.attr', 'aria-label', 'Clear filter')
      })
      cy.getByTestId('table-filters-clearAll').should('not.exist')
    })

    cy.get('input').type('1')
    cy.get('@setGlobalFilter').should('have.been.calledWith', 'text1')
    cy.getByTestId('table-search-clear').click()
    cy.get('@setGlobalFilter').should('have.been.calledWith', '')
  })

  it('should render the column filters', () => {
    const setGlobalFilter = cy.stub().as('setGlobalFilter')
    const resetColumnFilters = cy.stub().as('resetColumnFilters')

    cy.mountWithProviders(
      <SearchBar
        columnFilters={[]}
        globalFilter="text"
        enableColumnFilters
        setGlobalFilter={setGlobalFilter}
        resetColumnFilters={resetColumnFilters}
      />
    )

    cy.get('@setGlobalFilter').should('not.have.been.called')
    cy.get('@resetColumnFilters').should('not.have.been.called')

    cy.get('[role="toolbar"][aria-label="Search Toolbar"]').within(() => {
      cy.getByTestId('table-filters-clearAll').should('have.text', 'Clear all filters')
    })

    cy.getByTestId('table-filters-clearAll').click()
    cy.get('@resetColumnFilters').should('have.been.called')
    cy.get('@setGlobalFilter').should('have.been.calledWith', '')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SearchBar columnFilters={[]} globalFilter="text" enableColumnFilters enableGlobalFilter />)
    cy.checkAccessibility()
  })
})
