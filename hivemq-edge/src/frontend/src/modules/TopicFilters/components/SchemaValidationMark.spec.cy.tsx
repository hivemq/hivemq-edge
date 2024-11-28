import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import SchemaValidationMark from '@/modules/TopicFilters/components/SchemaValidationMark.tsx'

describe('SchemaValidationMark', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/sampling/topic/**', { items: [] }).as('getTopic')

    cy.intercept('/api/v1/management/sampling/schema/**', GENERATE_DATA_MODELS(true, MOCK_TOPIC_FILTER.topicFilter)).as(
      'getSchema'
    )
  })

  it('should render properly', () => {
    cy.mountWithProviders(<SchemaValidationMark topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('validation-loading').should('be.visible')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'success')
  })
})
