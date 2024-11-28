import DataModelSources from './DataModelSources.tsx'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('DataModelSources', () => {
  beforeEach(() => {
    cy.viewport(800, 900)

    cy.intercept('/api/v1/management/sampling/schema/**', GENERATE_DATA_MODELS(true, MOCK_TOPIC_FILTER.topicFilter)).as(
      'getSchema'
    )
    cy.intercept('/api/v1/management/sampling/topic/**', (req) => {
      req.reply({ items: [] })
    })
  })

  it('should render properly', () => {
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
