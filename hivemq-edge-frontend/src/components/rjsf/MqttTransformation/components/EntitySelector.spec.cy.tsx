import {
  SelectDestinationTag,
  SelectSourceTopics,
} from '@/components/rjsf/MqttTransformation/components/EntitySelector.tsx'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'

import type { DomainTagList } from '@/api/__generated__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

describe('SelectSourceTopics', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  describe('SelectSourceTopics', () => {
    it('should render properly', () => {
      cy.intercept('/api/v1/management/topic-filters', {
        items: [
          MOCK_TOPIC_FILTER,
          {
            topicFilter: 'another/filter',
            description: 'This is a topic filter',
          },
        ],
      }).as('getTopicFilters')

      cy.mountWithProviders(
        <SelectSourceTopics value="topic/test1" adapterId="my-adapter" adapterType="my-type" onChange={cy.stub()} />
      )

      cy.get('#mapping-select-source').should('contain.text', 'Loading...')
      cy.wait('@getTopicFilters')
      cy.get('#mapping-select-source').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-source').should('contain.text', 'topic/test1')
    })
  })

  describe('SelectSourceTopics', () => {
    it('should render properly', () => {
      const mockAdapterId = 'my-adapter'
      const mockResponse: DomainTagList = { items: MOCK_DEVICE_TAGS(mockAdapterId, MockAdapterType.SIMULATION) }
      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', mockResponse).as('types')

      cy.mountWithProviders(
        <SelectDestinationTag adapterId={mockAdapterId} adapterType="my-type" value="tag/test1" onChange={cy.stub()} />
      )

      // // Loading not working?
      // cy.get('#mapping-select-destination').should('contain.text', 'Loading...')
      // cy.get('#mapping-select-destination').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-destination').should('contain.text', 'tag/test1')
    })
  })
})
