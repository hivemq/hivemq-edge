import type { FC } from 'react'
import { Box } from '@chakra-ui/react'
import { ReactFlowProvider } from 'reactflow'

import config from '@/config'

import type { FiniteStateMachine } from '@datahub/types.ts'

import { MermaidRenderer } from '@datahub/components/fsm/MermaidRenderer.tsx'
import { ReactFlowRenderer } from '@datahub/components/fsm/ReactFlowRenderer.tsx'

export const FiniteStateMachineFlow: FC<FiniteStateMachine> = (props) => {
  if (config.features.DATAHUB_FSM_REACT_FLOW)
    return (
      <Box w="100%" height="400px">
        <ReactFlowProvider>
          <ReactFlowRenderer {...props} />
        </ReactFlowProvider>
      </Box>
    )
  return <MermaidRenderer {...props} />
}
