import type { FC } from 'react'
import { type HTMLAttributes, type ReactNode } from 'react'
import type { PanelPosition } from '@xyflow/react'
import { Panel as ReactFlowPanel } from '@xyflow/react'
import { Box } from '@chakra-ui/react'

// ReactFlow PanelProps is not exported
export type PanelProps = HTMLAttributes<HTMLDivElement> & {
  position: PanelPosition
  children: ReactNode
}

const Panel: FC<PanelProps> = ({ children, ...props }) => {
  return (
    <ReactFlowPanel {...props}>
      <Box
        sx={{
          borderColor: 'var(--chakra-colors-chakra-border-color)',
          borderWidth: 1,
          _dark: {
            backgroundColor: 'var(--chakra-colors-gray-700)',
          },
          borderRadius: 'var(--chakra-radii-md)',
          backgroundColor: 'var(--chakra-colors-chakra-body-bg)',
          boxShadow: 'var(--chakra-shadows-xl)',
        }}
      >
        {children}
      </Box>
    </ReactFlowPanel>
  )
}

export default Panel
