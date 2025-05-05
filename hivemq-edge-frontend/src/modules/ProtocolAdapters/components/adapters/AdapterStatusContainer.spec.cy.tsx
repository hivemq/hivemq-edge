import { AdapterStatusContainer } from '@/modules/ProtocolAdapters/components/adapters/AdapterStatusContainer.tsx'
import { mockAdapterConnectionStatus } from '@/api/hooks/useConnection/__handlers__'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

describe('AdapterStatusContainer', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('api/v1/management/protocol-adapters/status', { items: [mockAdapterConnectionStatus] }).as('getStatus')
  })

  it('should render properly an existing adapter', () => {
    cy.mountWithProviders(<AdapterStatusContainer id={MOCK_ADAPTER_ID} />)
    cy.getByTestId('connection-status').should('have.text', 'Connected')
  })

  it('should render unknown if not an adapter', () => {
    cy.mountWithProviders(<AdapterStatusContainer id="not-an-adapter" />)
    cy.getByTestId('connection-status').should('have.text', 'Unknown')
  })
})
