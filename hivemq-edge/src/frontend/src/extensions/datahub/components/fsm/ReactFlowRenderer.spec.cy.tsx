import type { FiniteStateMachineSchema } from '@datahub/types.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import { ReactFlowRenderer } from '@datahub/components/fsm/ReactFlowRenderer.tsx'
import { ReactFlowProvider } from '@xyflow/react'
import type { FC, PropsWithChildren } from 'react'
import { Box } from '@chakra-ui/react'

// @ts-ignore
const MOCK_FSM: FiniteStateMachineSchema = MOCK_BEHAVIOR_POLICY_SCHEMA.schema.definitions?.['Publish.quota']

const wrapper: FC<PropsWithChildren> = ({ children }) => (
  <Box w="100%" h={750}>
    <ReactFlowProvider>{children}</ReactFlowProvider>
  </Box>
)

describe('ReactFlowRenderer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the Mermaid diagram of the FSM', () => {
    cy.mountWithProviders(<ReactFlowRenderer {...MOCK_FSM.metadata} />, { wrapper })

    cy.get('.react-flow__nodes').find('[role="button"]').as('node+Edges')
    cy.get('@node+Edges').eq(0).should('contain.text', 'Connected')
  })
})
