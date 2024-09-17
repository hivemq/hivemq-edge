import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { HStack, Image, Text, VStack } from '@chakra-ui/react'

import logo from '@/assets/hivemq/01-hivemq-bee.svg'

import { BrokerClientConfiguration } from '@/api/types/api-broker-client.ts'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'
import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.ts'

const NodeClient: FC<NodeProps<BrokerClientConfiguration>> = ({ selected, data }) => {
  const { options } = useEdgeFlowContext()

  return (
    <>
      <NodeWrapper isSelected={selected} p={3} borderBottomRadius={30}>
        <VStack>
          {options.showTopics && data.subscriptions && (
            <MappingBadge destinations={data.subscriptions?.map((e) => e.destination)} />
          )}
          <HStack w="100%">
            <Image aria-label="client" boxSize="20px" objectFit="scale-down" src={logo} />
            <Text flex={1} data-testid="client-node-name">
              {data.id}
            </Text>
          </HStack>
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Top} isConnectable={false} />
    </>
  )
}

export default NodeClient
