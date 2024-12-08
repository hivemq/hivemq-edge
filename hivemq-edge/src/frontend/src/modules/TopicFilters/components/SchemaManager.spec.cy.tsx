import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import SchemaManager from '@/modules/TopicFilters/components/SchemaManager.tsx'

describe('SchemaManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    // cy.intercept('/api/v1/management/domain/topics/schema?*', (req) => {
    //   req.reply(GENERATE_DATA_MODELS(true, req.query.topics as string))
    // })
  })

  it.skip('should render loading errors', () => {
    cy.intercept('/api/v1/management/sampling/topic/**', {
      statusCode: 404,
      body: { title: 'The schema for the tags cannot be found', status: 404 },
    })
    cy.mountWithProviders(<SchemaManager topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('loading-spinner')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error').should('have.text', 'Not Found')
  })

  it.skip('should render validation errors', () => {
    cy.intercept('/api/v1/management/sampling/topic/**', { items: [] })
    cy.intercept('/api/v1/management/sampling/schema/**', {}).as('getSchema')
    cy.mountWithProviders(<SchemaManager topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('loading-spinner')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'No schema could be inferred from the traffic on this topic filter. Please try again later.')
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/sampling/topic/**', { items: [] })
    cy.intercept('/api/v1/management/sampling/schema/**', GENERATE_DATA_MODELS(true, MOCK_TOPIC_FILTER.topicFilter)).as(
      'getSchema'
    )
    cy.mountWithProviders(<SchemaManager topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('loading-spinner')
    cy.get('h4').should('contain.text', 'a/topic/+/filter')
    cy.get('[role="list"]').should('be.visible')
  })
})
