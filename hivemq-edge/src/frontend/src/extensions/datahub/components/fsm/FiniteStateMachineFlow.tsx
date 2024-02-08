import { FC } from 'react'
import { Box } from '@chakra-ui/react'

import { FiniteStateMachine } from '@datahub/types.ts'

import { MermaidRenderer } from '@datahub/components/fsm/MermaidRenderer.tsx'
import { ReactFlowRenderer } from '@datahub/components/fsm/ReactFlowRenderer.tsx'
import { ReactFlowProvider } from 'reactflow'

export const FiniteStateMachineFlow: FC<FiniteStateMachine> = (props) => {
  if (import.meta.env.VITE_FLAG_DATAHUB_STATIC_FSM === 'false')
    return (
      <Box w="100%" height="400px">
        <ReactFlowProvider>
          <ReactFlowRenderer {...props} />
        </ReactFlowProvider>
      </Box>
    )
  return <MermaidRenderer {...props} />
}
