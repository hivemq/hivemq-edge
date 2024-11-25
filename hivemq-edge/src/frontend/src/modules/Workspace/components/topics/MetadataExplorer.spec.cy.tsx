import MetadataExplorer from '@/modules/Workspace/components/topics/MetadataExplorer.tsx'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

describe('MetadataExplorer', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it.only('should render properly', () => {
    cy.intercept('/api/v1/management/sampling/schema/**', { configSchema: GENERATE_DATA_MODELS(true, 'test') }).as(
      'getSchema'
    )
    cy.mountWithProviders(<MetadataExplorer topic="test" />)

    cy.get('h2').should('contain.text', 'test')
    cy.getByTestId('loading-spinner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('button').should('have.attr', 'aria-label', 'Load samples').should('have.attr', 'disabled', 'disabled')

    cy.get('h4').should('have.length', 1)
    cy.get('h4').eq(0).should('contain.text', 'test')
  })

  it('should render error properly', () => {
    cy.intercept('/api/v1/management/sampling/topic/**', { statusCode: 404 }).as('getSchema')

    cy.mountWithProviders(<MetadataExplorer topic="test" />)

    cy.get('h2').should('contain.text', 'test')
    cy.getByTestId('loading-spinner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('button').should('have.attr', 'aria-label', 'Load samples').should('have.attr', 'disabled', 'disabled')

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'No sample could be observed for the topic filter test')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MetadataExplorer topic="test" />)

    cy.getByTestId('loading-spinner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.checkAccessibility()
    cy.percySnapshot('Component: MetadataExplorer')
  })
})
