import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_NODE_PULSE } from '@/__test-utils__/react-flow/nodes.ts'
import { PulseStatus } from '@/api/__generated__'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'

import NodePulse from '@/modules/Workspace/components/nodes/NodePulse.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'

describe('NodePulse', () => {
  beforeEach(() => {
    cy.viewport(600, 400)

    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/combiners', { statusCode: 202, log: false })
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodePulse {...MOCK_NODE_PULSE} />))
    cy.getByTestId('pulse-client-description').should('have.text', 'Pulse Client')
    cy.getByTestId('pulse-client-capabilities').within(() => {
      cy.get('svg').should('have.attr', 'data-type', PulseStatus.activationStatus.ACTIVATED)
    })
  })

  it('should render properly when deactivated', () => {
    cy.intercept('/api/v1/frontend/capabilities', { items: [] })
    cy.mountWithProviders(mockReactFlow(<NodePulse {...MOCK_NODE_PULSE} />))
    cy.getByTestId('pulse-client-capabilities').within(() => {
      cy.get('svg').should('have.attr', 'data-type', PulseStatus.activationStatus.DEACTIVATED)
    })
  })

  it('should render the selected adapter properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_PULSE, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.PULSE_NODE]: NodePulse }}
      />
    )
    cy.get('[role="toolbar"][data-id="idPulseClient"]').within(() => {
      cy.getByTestId('node-group-toolbar-panel').should('not.exist')
      cy.getByTestId('toolbar-title').should('have.text', 'my pulse client')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodePulse {...MOCK_NODE_PULSE} />))

    cy.checkAccessibility()
  })
})
