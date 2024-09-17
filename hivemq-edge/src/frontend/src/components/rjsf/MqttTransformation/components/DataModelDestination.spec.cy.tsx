import { MappingValidation } from '@/modules/Subscriptions/types.ts'
import DataModelDestination from './DataModelDestination.tsx'

const MOCK_SUBS: MappingValidation = {
  status: 'error',
  errors: [],
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('DataModelDestination', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<DataModelDestination topic="sssss" validation={MOCK_SUBS} />)

    cy.get('h3').should('have.text', 'Destination output')
    cy.get('[role=alert]').should('have.attr', 'data-status', 'error')

    // loading
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('loading-spinner').should('not.exist')
    cy.get('[role=list]').find('li').as('properties')
    cy.get('@properties').should('have.length', 6)

    cy.get('@properties')
      .eq(0)
      .should('have.text', 'First String')
      .should('have.attr', 'data-type', 'string')
      .should('not.have.attr', 'draggable')
    cy.get('@properties')
      .eq(1)
      .should('have.text', 'Second String')
      .should('have.attr', 'data-type', 'string')
      .should('not.have.attr', 'draggable')

    cy.get('@properties')
      .eq(2)
      .should('have.text', 'Number')
      .should('have.attr', 'data-type', 'integer')
      .should('not.have.attr', 'draggable')
  })

  it('should be accessible ', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DataModelDestination topic="sssss" validation={MOCK_SUBS} />, { wrapper })
    cy.checkAccessibility()
  })
})
