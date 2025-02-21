import type { FC } from 'react'
import type { NodeProps } from 'reactflow'
import { Handle, Position } from 'reactflow'
import { Box, Icon, Text } from '@chakra-ui/react'
import { MdScheduleSend } from 'react-icons/md'

import type { Combiner } from '@/api/__generated__'

import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'

import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'

const NodeCombiner: FC<NodeProps<Combiner>> = ({ id, selected, data, dragging }) => {
  const { onContextMenu } = useContextMenu(id, selected, '/workspace/combiner')

  return (
    <>
      <ContextualToolbar id={id} title={data.name} onOpenPanel={onContextMenu} dragging={dragging} />
      <NodeWrapper
        isSelected={selected}
        wordBreak="break-word"
        textAlign="center"
        borderTopRadius={30}
        borderBottomRadius={30}
        flexDirection="row"
        p={0}
        w={150}
        alignItems="center"
      >
        <Box p={4} backgroundColor="gray.300" borderTopLeftRadius={30} borderBottomLeftRadius={30}>
          <Icon as={MdScheduleSend} boxSize={6} />
        </Box>
        <Box flex={1} p={1} data-testid="combiner-description">
          <Text noOfLines={2}>{data.name}</Text>
        </Box>
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} />
      <Handle type="target" position={Position.Top} />
    </>
  )
}

export default NodeCombiner
