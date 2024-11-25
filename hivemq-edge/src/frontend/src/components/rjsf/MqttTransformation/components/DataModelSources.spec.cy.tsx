import DataModelSources from './DataModelSources.tsx'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('DataModelSources', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/domain/topics/schema?*', { test: GENERATE_DATA_MODELS(false, 'test') }).as(
      'getSchema'
    )
    cy.mountWithProviders(<DataModelSources topic="test" />)
    cy.get('h3').should('have.text', 'Sources')
    // loading
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('[role=list]').find('li').as('properties')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(<DataModelSources topic="sssss" />, { wrapper })

    cy.checkAccessibility()
  })
})
