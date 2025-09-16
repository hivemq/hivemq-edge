import type { FC } from 'react'
import { useMemo } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { Icon, Text, useColorModeValue, VStack } from '@chakra-ui/react'

import { EntityType } from '@/api/__generated__'

import { HqAssets, HqCombiner } from '@/components/Icons'
import { SelectEntityType } from '@/components/MQTT/types'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils'
import type { NodeCombinerType } from '@/modules/Workspace/types'
import MappingBadge from '../parts/MappingBadge'

const NodeCombiner: FC<NodeProps<NodeCombinerType>> = ({ id, selected, data, dragging }) => {
  const { onContextMenu } = useContextMenu(id, selected, `/workspace/combiner/${id}`)
  const bgColour = useColorModeValue('gray.300', 'gray.900')

  const topics = useMemo(() => {
    return data.mappings.items.map((e) => e.destination.topic as string)
  }, [data.mappings.items])

  const isAssetManager = useMemo(() => {
    return data.sources.items.some((e) => e.type === EntityType.PULSE_AGENT)
  }, [data.sources.items])

  return (
    <>
      <ContextualToolbar id={id} title={data.name} onOpenPanel={onContextMenu} dragging={dragging} />
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        wordBreak="break-word"
        textAlign="center"
        borderTopRadius={30}
        borderBottomRadius={30}
        flexDirection="row"
        p={0}
        w={CONFIG_ADAPTER_WIDTH}
        alignItems="center"
        h={120}
      >
        <VStack
          h="100%"
          p={4}
          backgroundColor={bgColour}
          borderTopLeftRadius={30}
          borderBottomLeftRadius={30}
          justifyContent="center"
        >
          <Icon as={isAssetManager ? HqAssets : HqCombiner} boxSize={10} />
        </VStack>
        <VStack p={2} h="100%" justifyContent="space-evenly">
          <Text data-testid="combiner-description" noOfLines={1}>
            {data.name}
          </Text>
          <MappingBadge destinations={topics} type={SelectEntityType.TOPIC} />
        </VStack>
      </NodeWrapper>
      <Handle type="source" id="Bottom" position={Position.Bottom} />
      <Handle type="target" id="Top" position={Position.Top} />
    </>
  )
}

export default NodeCombiner
