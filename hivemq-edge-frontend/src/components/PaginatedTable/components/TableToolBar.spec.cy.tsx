import TableToolBar from '@/components/PaginatedTable/components/TableToolBar.tsx'

describe('TableToolBar', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should not render when no tools', () => {
    cy.mountWithProviders(<TableToolBar />)

    cy.get('section').should('not.exist')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TableToolBar leftControls={<div>Left</div>} rightControls={<div>Right</div>} />)

    cy.get('section').should('have.attr', 'aria-label', 'Table Toolbar')
    cy.get('section div').should('have.length', 2)
    cy.get('section div').eq(0).should('contain.text', 'Left')
    cy.get('section div').eq(1).should('contain.text', 'Right')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TableToolBar rightControls={<div>Right</div>} />)

    cy.get('section').should('have.attr', 'aria-label', 'Table Toolbar')
    cy.get('section div').should('have.length', 1)
    cy.get('section div').eq(0).should('contain.text', 'Right')
  })
})
