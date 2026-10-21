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
    cy.intercept('GET', 'api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')
    cy.intercept('/api/v1/management/client/filters', { statusCode: 404 })
    cy.intercept('/api/v1/management/domain/tags/schema?*', GENERATE_DATA_MODELS(false, 'my-topic'))
    cy.intercept('/api/v1/management/domain/topics/schema?*', GENERATE_DATA_MODELS(true, 'test'))
    cy.intercept('/api/v1/management/topic-filters', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/sampling/schema/**', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/sampling/topic/**', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/schema/*/*', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', { statusCode: 202, log: false })
  })

  describe('method call settings', () => {
    it('should report methodCalls being switched on', () => {
      const onChange = cy.stub().as('onChange')

      cy.mountWithProviders(
        <MappingContainer
          onClose={cy.stub()}
          onSubmit={cy.stub()}
          onChange={onChange}
          item={MOCK_SOUTHBOUND_MAPPING}
          adapterId="my-adapter"
          adapterType="my-type"
        />,
        { wrapper }
      )

      cy.get('#mapping-methodCalls').should('not.be.checked')
      cy.get('@onChange').should('not.have.been.called')

      cy.get('#mapping-methodCalls').click({ force: true })
      cy.get('@onChange').should('have.been.calledWith', 'methodCalls', true)
    })

    it('should label each setting with the text from the schema', () => {
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

      cy.get('label[for="mapping-methodCalls"]').should('have.text', 'This mapping carries method calls')
      cy.get('label[for="mapping-correlationData"]').should('have.text', 'Requests carry correlation data')
      cy.get('label[for="mapping-responseTopic"]').should('have.text', 'Requests carry a response topic')

      cy.getByTestId('mapping-method-calls').should('contain.text', 'Read the method to call from each message')
    })

    it('should keep the dependent settings unusable while methodCalls is off', () => {
      cy.mountWithProviders(
        <MappingContainer
          onClose={cy.stub()}
          onSubmit={cy.stub()}
          onChange={cy.stub()}
          item={{ ...MOCK_SOUTHBOUND_MAPPING, methodCalls: false }}
          adapterId="my-adapter"
          adapterType="my-type"
        />,
        { wrapper }
      )

      cy.get('#mapping-methodCalls').should('not.be.disabled')
      cy.get('#mapping-correlationData').should('be.disabled')
      cy.get('#mapping-responseTopic').should('be.disabled')
    })

    it('should make the dependent settings usable once methodCalls is on', () => {
      const onChange = cy.stub().as('onChange')

      cy.mountWithProviders(
        <MappingContainer
          onClose={cy.stub()}
          onSubmit={cy.stub()}
          onChange={onChange}
          item={{ ...MOCK_SOUTHBOUND_MAPPING, methodCalls: true }}
          adapterId="my-adapter"
          adapterType="my-type"
        />,
        { wrapper }
      )

      cy.get('#mapping-methodCalls').should('be.checked')
      cy.get('#mapping-correlationData').should('not.be.disabled')
      cy.get('#mapping-responseTopic').should('not.be.disabled')

      cy.get('#mapping-correlationData').click({ force: true })
      cy.get('@onChange').should('have.been.calledWith', 'correlationData', true)

      cy.get('#mapping-responseTopic').click({ force: true })
      cy.get('@onChange').should('have.been.calledWith', 'responseTopic', true)
    })
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
  })
})
