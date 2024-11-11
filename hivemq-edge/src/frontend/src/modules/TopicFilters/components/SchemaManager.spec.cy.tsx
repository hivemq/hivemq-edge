import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import SchemaManager from '@/modules/TopicFilters/components/SchemaManager.tsx'

describe('SchemaManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/domain/topics/schema?*', (req) => {
      req.reply(GENERATE_DATA_MODELS(true, req.query.topics as string))
    })
  })

  it('should render loading errors', () => {
    cy.intercept('/api/v1/management/domain/topics/schema?*', { statusCode: 404 })
    cy.mountWithProviders(<SchemaManager topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('loading-spinner')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error').should('have.text', 'Not Found')
  })

  it('should render validation errors', () => {
    cy.intercept<Array<string>, string>('/api/v1/management/domain/topics/schema?*', {})
    cy.mountWithProviders(<SchemaManager topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('loading-spinner')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'No schema could be inferred from the traffic on this topic filter. Please try again later.')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<SchemaManager topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('loading-spinner')
    cy.get('h4').should('contain.text', 'a/topic/+/filter')
    cy.get('[role="list"]').should('be.visible')
  })
})
