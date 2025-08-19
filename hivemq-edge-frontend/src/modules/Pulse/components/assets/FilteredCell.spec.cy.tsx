import FilteredCell from '@/modules/Pulse/components/assets/FilteredCell.tsx'

describe('FilteredCell', () => {
  it('shows skeleton when loading', () => {
    cy.mountWithProviders(<FilteredCell isLoading value="AssetName" />)
    cy.get('.chakra-skeleton').should('be.visible').should('have.text', 'AssetName')
  })

  it('renders value when not loading', () => {
    cy.mountWithProviders(<FilteredCell isLoading={false} value="AssetName" />)
    cy.getByTestId('cell-content').should('have.text', 'AssetName')
    cy.getByTestId('cell-content').within(() => {
      cy.get('mark').should('not.exist')
    })
  })

  it('renders value when global filter not supported ', () => {
    cy.mountWithProviders(<FilteredCell canGlobalFilter={false} value="AssetName" />)
    cy.getByTestId('cell-content').should('have.text', 'AssetName')
    cy.getByTestId('cell-content').within(() => {
      cy.get('mark').should('not.exist')
    })
  })

  it("renders value when global filter don't match", () => {
    cy.mountWithProviders(<FilteredCell canGlobalFilter globalFilter="Fake" value="AssetName" />)
    cy.getByTestId('cell-content').should('have.text', 'AssetName')
    cy.getByTestId('cell-content').within(() => {
      cy.get('mark').should('not.exist')
    })
  })

  it('highlights value when global filter matches', () => {
    cy.mountWithProviders(<FilteredCell canGlobalFilter globalFilter="Name" value="AssetName" />)
    cy.getByTestId('cell-content').should('have.text', 'AssetName')
    cy.get('mark').should('have.text', 'Name')
  })
})
