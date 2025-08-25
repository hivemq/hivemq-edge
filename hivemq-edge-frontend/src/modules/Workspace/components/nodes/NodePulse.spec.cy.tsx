import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_NODE_PULSE } from '@/__test-utils__/react-flow/nodes.ts'
import { PulseStatus } from '@/api/__generated__'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_PULSE_ASSET_LIST } from '@/api/hooks/usePulse/__handlers__'

import NodePulse from '@/modules/Workspace/components/nodes/NodePulse.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { NODE_PULSE_AGENT_DEFAULT_ID } from '@/modules/Workspace/utils/nodes-utils.ts'

describe('NodePulse', () => {
  beforeEach(() => {
    cy.viewport(600, 400)

    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/combiners', { statusCode: 202, log: false })
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodePulse {...MOCK_NODE_PULSE} />))
    cy.getByTestId('pulse-client-description').should('have.text', 'Pulse Agent')
    cy.getByTestId('pulse-client-capabilities').within(() => {
      cy.get('svg').should('have.attr', 'data-type', PulseStatus.activation.ACTIVATED)
      cy.getByTestId('pulse-client-unmapped').should('have.text', 1)
      cy.getByTestId('pulse-client-mapped').should('have.text', 3)
    })

    cy.getByTestId('topics-container').within(() => {
      cy.getByTestId('topic-wrapper').should('have.length', 1)
      cy.getByTestId('topic-wrapper').eq(0).should('have.text', 'test / topic')
      cy.getByTestId('topic-wrapper').eq(0).find('svg').should('have.attr', 'aria-label', 'Asset')
    })
  })

  it('should render properly when deactivated', () => {
    cy.intercept('/api/v1/frontend/capabilities', { items: [] })
    cy.mountWithProviders(mockReactFlow(<NodePulse {...MOCK_NODE_PULSE} />))
    cy.getByTestId('pulse-client-capabilities').within(() => {
      cy.get('svg').should('have.attr', 'data-type', PulseStatus.activation.DEACTIVATED)
    })
  })

  it('should render the selected pulse node properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_PULSE, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.PULSE_NODE]: NodePulse }}
      />
    )

    cy.get('[role="toolbar"]').should('have.attr', 'data-id', NODE_PULSE_AGENT_DEFAULT_ID)
    cy.get('[role="toolbar"]').within(() => {
      cy.getByTestId('toolbar-title').should('have.text', 'my pulse client')
      cy.getByTestId('node-group-toolbar-panel').should('have.attr', 'aria-label', 'Open the overview panel')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodePulse {...MOCK_NODE_PULSE} />))

    cy.checkAccessibility()
  })
})
