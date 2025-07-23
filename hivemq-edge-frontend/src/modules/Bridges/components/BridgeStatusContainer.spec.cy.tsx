import { MOCK_BRIDGE_ID } from '@/__test-utils__/mocks.ts'
import { mockBridgeConnectionStatus } from '@/api/hooks/useConnection/__handlers__'
import { BridgeStatusContainer } from '@/modules/Bridges/components/BridgeStatusContainer.tsx'

describe('BridgeStatusContainer', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('api/v1/management/bridges/status', { items: [mockBridgeConnectionStatus] }).as('getStatus')
  })

  it('should render properly an existing bridge', () => {
    cy.mountWithProviders(<BridgeStatusContainer id={MOCK_BRIDGE_ID} />)

    cy.wait('@getStatus')
    cy.getByTestId('connection-status').should('have.text', 'Connected')
  })

  it('should render even with fetch error', () => {
    cy.intercept('/api/v1/management/bridges/status', { statusCode: 404 }).as('getStatus')
    cy.mountWithProviders(<BridgeStatusContainer id={MOCK_BRIDGE_ID} />)

    cy.wait('@getStatus')
    cy.getByTestId('connection-status').should('have.text', 'Unknown')
  })

  it('should render unknown if not a bridge', () => {
    cy.mountWithProviders(<BridgeStatusContainer id="not-a-bridge" />)

    cy.wait('@getStatus')
    cy.getByTestId('connection-status').should('have.text', 'Unknown')
  })
})
