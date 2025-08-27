import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import SourcesCell from '@/modules/Pulse/components/assets/SourcesCell.tsx'

describe('SourcesCell', () => {
  it('should render errors', () => {
    cy.intercept('GET', '/api/v1/management/combiners', { items: [MOCK_COMBINER_ASSET] })
    cy.mountWithProviders(<SourcesCell mappingId="wrong-combiner" />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('sources-error').should('have.text', '< not found >')
  })

  it('should render properly', () => {
    cy.intercept('GET', '/api/v1/management/combiners', { items: [MOCK_COMBINER_ASSET] }).as('getCombiner')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })

    cy.mountWithProviders(<SourcesCell mappingId="ff02efff-7b4c-4f8c-8bf6-74d0756283fb" />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getCombiner')
    cy.getByTestId('sources-container').within(() => {
      cy.getByTestId('node-name').should('have.text', 'my-adapter')
      cy.getByTestId('node-name').should('have.text', 'Pulse Agent')
    })
  })
})
