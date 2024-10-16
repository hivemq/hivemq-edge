import MetadataExplorer from '@/modules/Workspace/components/topics/MetadataExplorer.tsx'
import { mockClientSubscription } from '@/api/hooks/useClientSubscriptions/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

describe('MetadataExplorer', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/domain/topics/schema?*', { test: GENERATE_DATA_MODELS(true, 'test') }).as(
      'getSchema'
    )
    cy.intercept('/api/v1/management/client/filters', [mockClientSubscription]).as('getClients')
    cy.mountWithProviders(<MetadataExplorer topic="test" />)

    cy.get('h2').should('contain.text', 'test')
    cy.getByTestId('loading-spinner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('button').should('have.attr', 'aria-label', 'Load samples').should('have.attr', 'disabled', 'disabled')

    cy.get('h4').should('have.length', 1)
    cy.get('h4').eq(0).should('contain.text', 'test')
  })

  it('should render error properly', () => {
    cy.intercept('/api/v1/management/domain/topics/schema?*', {
      fakeTopic: GENERATE_DATA_MODELS(true, 'fakeTopic'),
    }).as('getSchema')
    cy.intercept('/api/v1/management/client/filters', [mockClientSubscription]).as('getClients')
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
