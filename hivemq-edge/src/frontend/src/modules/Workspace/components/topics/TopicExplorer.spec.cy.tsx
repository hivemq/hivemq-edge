import TopicExplorer from '@/modules/Workspace/components/topics/TopicExplorer.tsx'
import { mockISA95ApiBean } from '@/api/hooks/useUnifiedNamespace/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

describe('TopicExplorer', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
    cy.intercept('api/v1/management/uns/isa95', mockISA95ApiBean)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TopicExplorer />)

    cy.get('[role="tablist"]').find('[role="tab"]').as('tabs').should('have.length', 2)
    cy.get('@tabs').eq(0).should('have.text', 'Topic Wheel').should('have.attr', 'aria-selected', 'true')
    cy.get('@tabs').eq(1).should('have.text', 'Topic Tree View')

    cy.getByTestId('form-control-uns').should('have.text', 'UNS')
    cy.getByTestId('form-control-origin').should('have.text', 'Adapter')
    cy.get('svg > g > text').should('have.text', 'HiveMQ Edge')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TopicExplorer />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: TopicExplorer')
  })
})
