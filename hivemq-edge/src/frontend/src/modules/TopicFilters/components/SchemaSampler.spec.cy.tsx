import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import SchemaSampler from '@/modules/TopicFilters/components/SchemaSampler.tsx'

describe('SchemaSampler', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it.skip('should render loading errors', () => {
    cy.intercept('/api/v1/management/sampling/topic/**', {
      statusCode: 404,
      body: { title: 'The schema for the tags cannot be found', status: 404 },
    })
    cy.mountWithProviders(<SchemaSampler topicFilter={MOCK_TOPIC_FILTER} onUpload={cy.stub()} />)

    cy.getByTestId('loading-spinner')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error').should('have.text', 'Not Found')
  })

  it.skip('should render validation errors', () => {
    cy.intercept('/api/v1/management/sampling/topic/**', { items: [] })
    cy.intercept('/api/v1/management/sampling/schema/**', {}).as('getSchema')
    cy.mountWithProviders(<SchemaSampler topicFilter={MOCK_TOPIC_FILTER} onUpload={cy.stub()} />)

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
    const onUpload = cy.stub().as('onUpload')
    cy.mountWithProviders(<SchemaSampler topicFilter={MOCK_TOPIC_FILTER} onUpload={onUpload} />)

    cy.getByTestId('loading-spinner')
    cy.get('h4').should('contain.text', 'a/topic/+/filter')
    cy.get('[role="list"]').should('be.visible')

    cy.get('@onUpload').should('not.have.been.called')
    cy.getByTestId('schema-sampler-upload').should('have.text', 'Assign schema').click()
    cy.get('@onUpload').should('have.been.calledWithMatch', 'data:application/json;base64,')
  })
})
