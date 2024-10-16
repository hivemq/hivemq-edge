import {
  SelectDestinationTag,
  SelectSourceTopics,
} from '@/components/rjsf/MqttTransformation/components/EntitySelector.tsx'
import { MOCK_DEVICE_TAGS, mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockClientSubscription } from '@/api/hooks/useClientSubscriptions/__handlers__'
import { DomainTagList } from '@/api/__generated__'

describe('SelectSourceTopics', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  describe('SelectSourceTopics', () => {
    it('should render properly', () => {
      cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
      cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
      cy.intercept('/api/v1/management/bridges', { items: [mockBridge] })
      cy.intercept('/api/v1/management/client/filters', [mockClientSubscription])

      cy.mountWithProviders(<SelectSourceTopics values={['topic/test1', 'topic/test2']} onChange={cy.stub()} />)

      cy.get('#mapping-select-source').should('contain.text', 'Loading...')
      cy.get('#mapping-select-source').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-source').find('[data-testid="topic-wrapper"]').as('topics')
      cy.get('@topics').eq(0).should('have.text', 'topic/test1')
      cy.get('@topics').eq(1).should('have.text', 'topic/test2')
    })
  })

  describe('SelectSourceTopics', () => {
    it('should render properly', () => {
      const mockAdapterId = 'my-adapter'
      const mockResponse: DomainTagList = { items: MOCK_DEVICE_TAGS(mockAdapterId) }
      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', mockResponse)

      cy.mountWithProviders(
        <SelectDestinationTag adapterId={mockAdapterId} values={['tag/test1']} onChange={cy.stub()} />
      )

      // // Loading
      // cy.get('#mapping-select-destination').should('contain.text', 'Loading...')
      // cy.get('#mapping-select-destination').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-destination').should('contain.text', 'tag/test1')
    })
  })
})
