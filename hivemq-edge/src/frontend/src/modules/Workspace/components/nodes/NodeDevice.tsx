import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { HStack, Icon, Text, VStack } from '@chakra-ui/react'

import { DeviceMetadata } from '@/modules/Workspace/types.ts'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { deviceCapabilityIcon, deviceCategoryIcon } from '@/modules/Workspace/utils/adapter.utils.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'

const NodeDevice: FC<NodeProps<DeviceMetadata>> = ({ id, selected, data }) => {
  const { onContextMenu } = useContextMenu(id, selected, '/workspace/node')
  const { category, capabilities } = data
  const showSkeleton = useStore(selectorIsSkeletonZoom)

  return (
    <>
      <ContextualToolbar id={id} onOpenPanel={onContextMenu} />
      <NodeWrapper
        isSelected={selected}
        wordBreak="break-word"
        maxW={200}
        textAlign="center"
        p={3}
        borderTopRadius={30}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
      >
        <VStack>
          <HStack w="100%" justifyContent="flex-end" gap={1} data-testid="device-capabilities">
            {capabilities?.map((capability) => (
              <Icon key={capability} boxSize={4} as={deviceCapabilityIcon[capability]} data-type={capability} />
            ))}
          </HStack>
          <HStack w="100%" data-testid="device-description">
            <Icon as={deviceCategoryIcon[category?.name || 'SIMULATION']} data-type={category?.name} />
            <Text>{data.protocol}</Text>
          </HStack>
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NodeDevice
