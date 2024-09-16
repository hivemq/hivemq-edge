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

    // cy.get('[role=list]').find('li').as('properties')
    // cy.get('@properties').should('have.length', 13)
    //
    // cy.get('@properties')
    //   .eq(0)
    //   .should('have.text', 'firstName')
    //   .should('have.attr', 'data-type', 'string')
    //   .should('have.attr', 'draggable', 'true')
    //
    // cy.get('@properties')
    //   .eq(2)
    //   .should('have.text', 'age')
    //   .should('have.attr', 'data-type', 'integer')
    //   .should('have.attr', 'draggable', 'true')
    //
    // cy.get('@properties')
    //   .eq(6)
    //   .should('have.text', 'listOfStrings')
    //   .should('have.attr', 'data-type', 'array')
    //   .should('have.attr', 'draggable', 'true')
    //
    // cy.get('@properties')
    //   .eq(7)
    //   .should('have.text', '___index')
    //   .should('have.attr', 'data-type', 'string')
    //   .should('have.attr', 'data-path', 'listOfStrings.___index')
    //   .should('have.attr', 'draggable', 'true')

    // TODO[NVL] Cannot test MQTTClient. Need a better mock handling
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'No sample could be observed for the topic filter #')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(<DataModelSources topics={['sssss']} />, { wrapper })

    cy.checkAccessibility()
  })
})
