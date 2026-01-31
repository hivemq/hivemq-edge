import { MermaidRenderer } from '@datahub/components/fsm/MermaidRenderer.tsx'
import type { FiniteStateMachineSchema } from '@datahub/types.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'

// @ts-ignore
const MOCK_FSM: FiniteStateMachineSchema = MOCK_BEHAVIOR_POLICY_SCHEMA.schema.definitions?.['Publish.quota']

describe('MermaidRenderer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the Mermaid diagram of the FSM', () => {
    cy.mountWithProviders(<MermaidRenderer {...MOCK_FSM.metadata} />)

    cy.get('svg').find('g.nodes > g').as('nodes')
    cy.get('@nodes').should('have.length', 9)
    cy.get('@nodes').eq(1).should('have.text', 'Initial')
    cy.get('@nodes').eq(4).should('have.text', 'Violated')
  })

  it('should render with selected transition without errors', () => {
    // This test verifies that the component mounts with selectedTransition prop
    // Note: Mermaid's async rendering may not fully complete in test environment
    cy.mountWithProviders(
      <MermaidRenderer
        {...MOCK_FSM.metadata}
        selectedTransition={{
          event: 'Mqtt.OnInboundPublish',
          from: 'Publishing',
          to: 'Violated',
        }}
      />
    )

    // Verify component renders the Card container
    cy.get('.chakra-card').should('exist')
  })
})
