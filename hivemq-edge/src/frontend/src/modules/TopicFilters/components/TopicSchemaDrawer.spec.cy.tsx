import { Button } from '@chakra-ui/react'

import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import TopicSchemaDrawer from '@/modules/TopicFilters/components/TopicSchemaDrawer.tsx'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

describe('TopicSchemaDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/domain/topics/schema?*', (req) => {
      req.reply(GENERATE_DATA_MODELS(true, req.query.topics as string))
    })
  })

  it('should render properly', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <TopicSchemaDrawer
        topicFilter={MOCK_TOPIC_FILTER}
        trigger={({ onOpen }) => (
          <Button data-testid="trigger" onClick={onOpen}>
            Click me!
          </Button>
        )}
      />
    )

    cy.get('[role="dialog"]').should('not.exist')
    cy.getByTestId('trigger').click()
    cy.get('[role="dialog"]').should('be.visible')
    cy.get('header').should('contain.text', 'Manage the schemas of the topic filter')
    cy.checkAccessibility()
  })
})
