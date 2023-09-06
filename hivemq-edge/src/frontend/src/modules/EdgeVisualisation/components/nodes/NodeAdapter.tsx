import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { Box, HStack, Image, Text, VStack, type BoxProps } from '@chakra-ui/react'
import { useNavigate } from 'react-router-dom'

import { Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import TopicsContainer from '../parts/TopicsContainer.tsx'
import { getAdapterTopics } from '../../utils/topics-utils.ts'
import { CONFIG_ADAPTER_WIDTH } from '../../utils/nodes-utils.ts'
import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'

const NodeAdapter: FC<NodeProps<Adapter>> = ({ id, data: adapter, selected }) => {
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol = protocols?.items?.find((e) => e.id === adapter.type)
  const topics = getAdapterTopics(adapter)
  const { options } = useEdgeFlowContext()
  const navigate = useNavigate()

  const selectedStyle: Partial<BoxProps> = {
    boxShadow: 'dark-lg',
    rounded: 'md',
    bg: '#dddfe2',
  }

  return (
    <>
      <NodeWrapper p={2} {...(selected ? { ...selectedStyle } : {})} w={CONFIG_ADAPTER_WIDTH}>
        <VStack onDoubleClick={() => navigate(`/edge-flow/node/${id}`)}>
          <HStack w={'100%'}>
            <Image aria-label={adapter.type} boxSize="20px" objectFit="scale-down" src={adapterProtocol?.logoUrl} />
            <Text flex={1} data-testid={'adapter-node-name'}>
              {adapter.id}
            </Text>
          </HStack>
          {options.showStatus && (
            <Box flex={1}>
              <ConnectionStatusBadge status={adapter.adapterRuntimeInformation?.connectionStatus?.status} />
            </Box>
          )}
          {options.showTopics && <TopicsContainer topics={topics} />}
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={true} />
    </>
  )
}

export default NodeAdapter
