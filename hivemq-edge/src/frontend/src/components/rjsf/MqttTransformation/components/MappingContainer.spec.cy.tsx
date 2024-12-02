import MappingContainer from './MappingContainer.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_SOUTHBOUND_MAPPING } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('MappingContainer', () => {
  beforeEach(() => {
    cy.viewport(1200, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')
    cy.intercept('/api/v1/management/client/filters', { statusCode: 404 })
    cy.intercept('/api/v1/management/domain/tags/schema?*', GENERATE_DATA_MODELS(false, 'my-topic'))
    cy.intercept('/api/v1/management/domain/topics/schema?*', GENERATE_DATA_MODELS(true, 'test'))
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <MappingContainer
        onClose={cy.stub()}
        onSubmit={cy.stub()}
        onChange={cy.stub()}
        item={MOCK_SOUTHBOUND_MAPPING}
        adapterId="my-adapter"
        adapterType="my-type"
      />,
      { wrapper }
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: MappingContainer')
  })
})
