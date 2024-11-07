import TopicFilterManager from '@/modules/TopicFilters/TopicFilterManager.tsx'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

describe('TopicFilterManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        MOCK_TOPIC_FILTER,
        {
          topicFilter: 'another/filter',
          description: 'This is a topic filter',
        },
      ],
    }).as('getTopicFilters')
    cy.intercept('/api/v1/management/domain/topics/schema?*', (req) => {
      if (req.query.topics === 'another/filter') req.reply({})
      else req.reply(GENERATE_DATA_MODELS(true, req.query.topics as string))
    })
  })

  it('should render properly', () => {
    cy.injectAxe()

    cy.mountWithProviders(<TopicFilterManager />)

    cy.get('header').should('have.text', 'Manage topic filters')
    cy.get('table').should('be.visible')

    cy.get('table').as('table').should('have.attr', 'aria-label', 'List of topic filters')
    cy.get('@table').find('thead tr').eq(0).find('th').as('rowHeader').should('have.length', 4)
    cy.get('@rowHeader').eq(0).should('have.text', 'Topic Filter')
    cy.get('@rowHeader').eq(1).should('have.text', 'Description')
    cy.get('@rowHeader').eq(2).should('have.text', 'Schema')
    cy.get('@rowHeader').eq(3).should('have.text', 'Actions')

    cy.get('@table').find('tbody tr').as('body').should('have.length', 2)
    cy.get('@body').find('td').eq(0).should('have.text', 'a / topic / + / filter')
    cy.get('@body').find('td').eq(1).should('have.text', 'This is a topic filter')
    cy.get('@body').find('td').eq(2).children().should('have.attr', 'data-status', 'success')
    cy.get('@body').find('td').eq(6).children().should('have.attr', 'data-status', 'error')

    cy.get('@body').find('td').eq(3).find('button').as('topicActions')

    cy.get('@topicActions').eq(0).should('have.attr', 'aria-label', 'Manage Schemas')
    cy.get('@topicActions').eq(1).should('have.attr', 'aria-label', 'Edit Topic Filter').should('be.disabled', true)
    cy.get('@topicActions').eq(2).should('have.attr', 'aria-label', 'Delete Topic Filter').should('be.disabled', true)
    cy.checkAccessibility()
  })
})
