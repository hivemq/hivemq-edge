import { FC } from 'react'
import { NodeToolbar as ReactFlowNodeToolbar, NodeToolbarProps } from 'reactflow'
import { As, chakra, Icon } from '@chakra-ui/react'
import { BsGripVertical } from 'react-icons/bs'

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
        paddingRight: 2,
        borderRadius: 'var(--chakra-radii-md)',
        backgroundColor: 'var(--chakra-colors-chakra-body-bg)',
        boxShadow: 'var(--chakra-shadows-dark-lg)',
      }}
    >
      <Icon as={BsGripVertical} boxSize={7} aria-hidden={true} />
      {children}
    </NodeToolbarChakra>
  )
}

export default NodeToolbar
