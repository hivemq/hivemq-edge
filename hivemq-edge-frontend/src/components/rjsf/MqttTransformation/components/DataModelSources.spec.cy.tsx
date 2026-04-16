import DataModelSources from './DataModelSources.tsx'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

describe('DataModelSources', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('/api/v1/management/topic-filters', { items: [MOCK_TOPIC_FILTER] }).as('getTopicFilters')
  })

  it('should render the schema when the topic filter has a valid schema assigned', () => {
    cy.mountWithProviders(<DataModelSources topic={MOCK_TOPIC_FILTER.topicFilter} />)
    cy.wait('@getTopicFilters')
    cy.get('h3').first().should('have.text', 'Sources')
    cy.get('[role=list]').find('li').should('have.length.greaterThan', 0)
  })

  it('should show a warning when the topic filter has no schema assigned', () => {
    cy.intercept('/api/v1/management/topic-filters', {
      items: [{ ...MOCK_TOPIC_FILTER, schema: undefined }],
    }).as('getTopicFiltersNoSchema')
    cy.mountWithProviders(<DataModelSources topic={MOCK_TOPIC_FILTER.topicFilter} />)
    cy.wait('@getTopicFiltersNoSchema')
    cy.get('[role=alert]').should('be.visible')
    cy.get('[role=alert]').should('contain', 'not assigned a schema')
  })

  it('should show a warning when the selected topic is not found in the topic filter list', () => {
    cy.mountWithProviders(<DataModelSources topic="unknown/topic" />)
    cy.wait('@getTopicFilters')
    cy.get('[role=alert]').should('be.visible')
    cy.get('[role=alert]').should('contain', 'not assigned a schema')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DataModelSources topic={MOCK_TOPIC_FILTER.topicFilter} />)
    cy.wait('@getTopicFilters')
    cy.checkAccessibility(undefined, {
      rules: {
        // h5 used for sections in JsonSchemaBrowser is not in order in test context
        'heading-order': { enabled: false },
      },
    })
  })
})
