import {
  SelectDestinationTag,
  SelectSourceTopics,
} from '@/components/rjsf/MqttTransformation/components/EntitySelector.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

describe('SelectSourceTopics', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  describe('SelectSourceTopics', () => {
    it('should render properly', () => {
      cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
      cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
      cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')

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
      cy.mountWithProviders(<SelectDestinationTag values={['tag/test1']} onChange={cy.stub()} />)

      // Loading
      cy.get('#mapping-select-destination').should('contain.text', 'Loading...')
      cy.get('#mapping-select-destination').should('not.contain.text', 'Loading...')

      cy.get('#mapping-select-destination').should('contain.text', 'tag/test1')
    })
  })
})
