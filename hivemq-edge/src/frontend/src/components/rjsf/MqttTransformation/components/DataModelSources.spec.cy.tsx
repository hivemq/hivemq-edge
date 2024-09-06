import DataModelSources from './DataModelSources.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('DataModelSources', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<DataModelSources topics={['sssss']} />)
    cy.get('h3').should('have.text', 'Sources')
    // loading
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('[role=list]').find('li').as('properties')
    cy.get('@properties').should('have.length', 13)

    cy.get('@properties').eq(0).should('have.text', 'firstName').should('have.attr', 'data-type', 'string')
    cy.get('@properties').eq(2).should('have.text', 'age').should('have.attr', 'data-type', 'integer')
    cy.get('@properties').eq(6).should('have.text', 'listOfStrings').should('have.attr', 'data-type', 'array')
    cy.get('@properties')
      .eq(7)
      .should('have.text', '___index')
      .should('have.attr', 'data-type', 'string')
      .should('have.attr', 'data-path', 'listOfStrings.___index')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(<DataModelSources topics={['sssss']} />, { wrapper })

    cy.checkAccessibility()
  })
})
