import type { FC } from 'react'
import type { Node, NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { Badge, Box, Text, VStack, useColorModeValue } from '@chakra-ui/react'

import type { NetworkGraphNodeData } from '@/modules/DomainOntology/hooks/useGetNetworkGraphData.ts'

const NODE_COLORS = {
  TAG: {
    bg: 'blue.50',
    border: 'blue.400',
    badge: 'blue',
  },
  TOPIC: {
    bg: 'green.50',
    border: 'green.400',
    badge: 'green',
  },
  TOPIC_FILTER: {
    bg: 'purple.50',
    border: 'purple.400',
    badge: 'purple',
  },
}

const NetworkGraphNode: FC<NodeProps<Node<NetworkGraphNodeData>>> = ({ data, selected }) => {
  const colors = NODE_COLORS[data.type]
  const bgColor = useColorModeValue(colors.bg, `${colors.badge}.900`)
  const borderColor = useColorModeValue(colors.border, `${colors.badge}.400`)
  const selectedBorderColor = useColorModeValue(`${colors.badge}.600`, `${colors.badge}.300`)

  // Scale node size based on connection count (min 120px, max 240px)
  const baseSize = 120
  const maxSize = 240
  const sizeMultiplier = Math.min(data.connectionCount / 10, 1)
  const nodeWidth = baseSize + (maxSize - baseSize) * sizeMultiplier

  return (
    <>
      <Handle type="target" position={Position.Top} isConnectable={false} />
      <Box
        bg={bgColor}
        borderWidth={selected ? 3 : 2}
        borderColor={selected ? selectedBorderColor : borderColor}
        borderRadius="md"
        p={3}
        minW={`${nodeWidth}px`}
        maxW="300px"
        boxShadow={selected ? 'lg' : 'md'}
        cursor="pointer"
        transition="all 0.2s"
        _hover={{
          boxShadow: 'lg',
          transform: 'scale(1.05)',
        }}
      >
        <VStack spacing={2} align="stretch">
          <Badge colorScheme={colors.badge} fontSize="xs" alignSelf="flex-start">
            {data.type}
          </Badge>
          <Text fontSize="sm" fontWeight="medium" noOfLines={2} wordBreak="break-word">
            {data.label}
          </Text>
          {data.connectionCount > 0 && (
            <Text fontSize="xs" color="gray.500">
              {data.connectionCount} connection{data.connectionCount > 1 ? 's' : ''}
            </Text>
          )}
        </VStack>
      </Box>
      <Handle type="source" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NetworkGraphNode
