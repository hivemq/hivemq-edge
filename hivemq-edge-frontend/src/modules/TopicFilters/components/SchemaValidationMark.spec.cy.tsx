import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import SchemaValidationMark from '@/modules/TopicFilters/components/SchemaValidationMark.tsx'

describe('SchemaValidationMark', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<SchemaValidationMark topicFilter={MOCK_TOPIC_FILTER} />)

    cy.get('[role="alert"]').should('have.attr', 'data-status', 'success')
  })
})
