import { FC, type HTMLAttributes, type ReactNode } from 'react'
import { Panel as ReactFlowPanel, PanelPosition } from 'reactflow'
import { HStack } from '@chakra-ui/react'

// ReactFlow PanelProps is not exported
export type PanelProps = HTMLAttributes<HTMLDivElement> & {
  position: PanelPosition
  children: ReactNode
}

const Panel: FC<PanelProps> = ({ children, ...props }) => {
  return (
    <ReactFlowPanel {...props}>
      <HStack
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
      </HStack>
    </ReactFlowPanel>
  )
}

export default Panel
