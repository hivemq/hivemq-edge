import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { HStack, Image, Text, VStack } from '@chakra-ui/react'

import logo from '@/assets/hivemq/01-hivemq-bee.svg'

import { BrokerClientConfiguration } from '@/api/types/api-broker-client.ts'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import TopicsContainer from '@/modules/Workspace/components/parts/TopicsContainer.tsx'
import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.ts'

const NodeClient: FC<NodeProps<BrokerClientConfiguration>> = ({ id, selected, data }) => {
  const { options } = useEdgeFlowContext()

  return (
    <>
      <ContextualToolbar id={id} />
      <NodeWrapper isSelected={selected} p={3} borderBottomRadius={30}>
        <VStack>
          {options.showTopics && data.subscription && (
            <TopicsContainer topics={data.subscription?.map((e) => ({ topic: e.destination }))} />
          )}
          <HStack w="100%">
            <Image aria-label="client" boxSize="20px" objectFit="scale-down" src={logo} />
            <Text flex={1} data-testid="adapter-node-name">
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
