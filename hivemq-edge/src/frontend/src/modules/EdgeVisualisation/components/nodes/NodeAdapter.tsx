import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { Box, HStack, Image, Text, VStack } from '@chakra-ui/react'

import { Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import TopicsContainer from '../parts/TopicsContainer.tsx'
import { getAdapterTopics } from '../../utils/topics-utils.ts'
import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'

const NodeAdapter: FC<NodeProps<Adapter>> = ({ data: adapter }) => {
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol = protocols?.items?.find((e) => e.id === adapter.type)
  const topics = getAdapterTopics(adapter)
  const { options } = useEdgeFlowContext()

  return (
    <>
      <NodeWrapper p={2}>
        <VStack>
          <HStack w={'100%'}>
            <Image boxSize="20px" objectFit="scale-down" src={adapterProtocol?.logoUrl} />
            <Text flex={1}>{adapter.id} </Text>
          </HStack>
          <Box flex={1}>
            <ConnectionStatusBadge status={adapter.adapterRuntimeInformation?.connectionStatus?.status} />
          </Box>
          {options.showTopics && <TopicsContainer topics={topics} />}
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={true} />
    </>
  )
}

export default NodeAdapter
