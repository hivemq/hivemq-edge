import { OutwardMapping } from '@/modules/Mappings/types.ts'
import MappingContainer from './MappingContainer.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

const MOCK_SUBS: OutwardMapping = {
  tag: 'my-tag',
  mqttTopicFilter: 'my-topic',
  fieldMapping: [{ source: { propertyPath: 'dropped-property' }, destination: { propertyPath: 'Second String' } }],
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('SubscriptionContainer', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <MappingContainer onClose={cy.stub()} onSubmit={cy.stub()} onChange={cy.stub()} item={MOCK_SUBS} />,
      { wrapper }
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: SubscriptionContainer')
  })
})
