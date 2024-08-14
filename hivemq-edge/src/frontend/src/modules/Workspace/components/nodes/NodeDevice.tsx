import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { HStack, Icon, Text, VStack } from '@chakra-ui/react'

import { ProtocolAdapter } from '@/api/__generated__'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { deviceCapabilityIcon, deviceCategoryIcon } from '@/modules/Workspace/utils/adapter.utils.ts'

const NodeDevice: FC<NodeProps<ProtocolAdapter>> = ({ selected, data }) => {
  const { category, capabilities } = data
  return (
    <>
      <NodeWrapper
        isSelected={selected}
        wordBreak="break-word"
        maxW={200}
        textAlign="center"
        p={3}
        borderTopRadius={30}
      >
        <VStack>
          <HStack w="100%" justifyContent="flex-end" gap={1}>
            {capabilities?.map((e) => (
              <Icon key={e} boxSize={4} as={deviceCapabilityIcon[e]} />
            ))}
          </HStack>
          <HStack w="100%">
            <Icon as={deviceCategoryIcon[category?.name || 'SIMULATION']} />
            <Text>{data.protocol}</Text>
          </HStack>
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NodeDevice
