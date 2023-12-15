import { Box, VisuallyHidden, useTheme } from '@chakra-ui/react'
import { FC } from 'react'
import { useNavigate } from 'react-router-dom'
import { Handle, NodeProps, Position } from 'reactflow'

const NodeGroup: FC<NodeProps<string>> = ({ id, data }) => {
  const navigate = useNavigate()
  const { colors } = useTheme()

  return (
    <>
      <Box
        w={'100%'}
        h={'100%'}
        style={{
          backgroundColor: colors.red[500],
          borderRadius: '100%',
          opacity: 0.05,
          borderColor: 'red',
          borderWidth: 3,
          borderStyle: 'solid',
        }}
        onDoubleClick={() => navigate(`/edge-flow/group/${id}`)}
      >
        <VisuallyHidden>{data}</VisuallyHidden>
      </Box>
      <Handle type="source" position={Position.Bottom} id="a" />
    </>
  )
}

export default NodeGroup
