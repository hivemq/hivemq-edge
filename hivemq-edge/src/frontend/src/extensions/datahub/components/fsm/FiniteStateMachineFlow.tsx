import { FC } from 'react'

import { FiniteStateMachine } from '@datahub/types.ts'

import { MermaidRenderer } from '@datahub/components/fsm/MermaidRenderer.tsx'
import { ReactFlowRenderer } from '@datahub/components/fsm/ReactFlowRenderer.tsx'
import { Box } from '@chakra-ui/react'

export const FiniteStateMachineFlow: FC<FiniteStateMachine> = (props) => {
  if (import.meta.env.VITE_FLAG_DATAHUB_STATIC_FSM === 'true') return <MermaidRenderer {...props} />
  return (
    <Box w="100%" height="400px">
      <ReactFlowRenderer {...props} />
    </Box>
  )
}
