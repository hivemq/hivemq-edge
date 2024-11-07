import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import SchemaValidationMark from '@/modules/TopicFilters/components/SchemaValidationMark.tsx'

describe('SchemaValidationMark', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/domain/topics/schema?*', (req) => {
      req.reply(GENERATE_DATA_MODELS(true, req.query.topics as string))
    })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<SchemaValidationMark topicFilter={MOCK_TOPIC_FILTER} />)

    cy.getByTestId('validation-loading').should('be.visible')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'success')
  })

  it('should render error', () => {
    cy.mountWithProviders(<SchemaValidationMark topicFilter={{}} />)

    cy.getByTestId('validation-loading').should('be.visible')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error')
  })
})
