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
      cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('types')
      cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('adapters')
      cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('bridges')
      cy.intercept('/api/v1/management/client/filters', [mockClientSubscription]).as('clients')

      cy.mountWithProviders(<SelectSourceTopics value="topic/test1" onChange={cy.stub()} />)

      cy.get('#mapping-select-source').should('contain.text', 'Loading...')
      cy.wait('@types')
      cy.wait('@adapters')
      cy.wait('@bridges')
      cy.wait('@clients')
      cy.get('#mapping-select-source').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-source').should('contain.text', 'topic/test1')
    })
  })

  describe('SelectSourceTopics', () => {
    it('should render properly', () => {
      const mockAdapterId = 'my-adapter'
      const mockResponse: DomainTagList = { items: MOCK_DEVICE_TAGS(mockAdapterId) }
      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', mockResponse).as('types')

      cy.mountWithProviders(<SelectDestinationTag adapterId={mockAdapterId} value="tag/test1" onChange={cy.stub()} />)

      // // Loading not working?
      // cy.get('#mapping-select-destination').should('contain.text', 'Loading...')
      // cy.get('#mapping-select-destination').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-destination').should('contain.text', 'tag/test1')
    })
  })
})
