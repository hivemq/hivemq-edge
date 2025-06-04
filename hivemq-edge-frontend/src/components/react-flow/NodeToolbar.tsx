import type { FC } from 'react'
import type { NodeToolbarProps } from '@xyflow/react'
import { NodeToolbar as ReactFlowNodeToolbar } from '@xyflow/react'
import type { As } from '@chakra-ui/react'
import { chakra } from '@chakra-ui/react'

const NodeToolbarChakra = chakra<As, NodeToolbarProps>(ReactFlowNodeToolbar)

const NodeToolbar: FC<NodeToolbarProps> = ({ children, ...props }) => {
  return (
    <NodeToolbarChakra
      offset={8}
      {...props}
      role="toolbar"
      display="flex"
      flexDirection="row"
      alignItems="center"
      height="50px"
      gap="0.5em"
      sx={{
        borderColor: 'var(--chakra-colors-chakra-border-color)',
        borderWidth: 1,
        _dark: {
          backgroundColor: 'var(--chakra-colors-gray-700)',
        },
        padding: 2,
        borderRadius: 'var(--chakra-radii-md)',
        backgroundColor: 'var(--chakra-colors-chakra-body-bg)',
        boxShadow: 'var(--chakra-shadows-dark-lg)',
      }}
    >
      {children}
    </NodeToolbarChakra>
  )
}

export default NodeToolbar
