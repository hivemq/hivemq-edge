import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { HStack, Text, VStack } from '@chakra-ui/react'

import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { BrokerClientConfiguration } from '@/api/types/api-broker-client.ts'

const NodeClient: FC<NodeProps<BrokerClientConfiguration>> = ({ selected, data }) => {
  return (
    <>
      <NodeWrapper
        isSelected={selected}
        wordBreak="break-word"
        maxW={200}
        textAlign="center"
        p={3}
        borderRightRadius={30}
      >
        <VStack>
          <HStack w="100%" data-testid="device-description">
            <Text>{data.id}</Text>
          </HStack>
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Left} isConnectable={false} />
    </>
  )
}

export default NodeClient
