import { FC, useMemo } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { Box, HStack, Image, Text, VStack } from '@chakra-ui/react'

import { Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import TopicsContainer from '../parts/TopicsContainer.tsx'
import { discoverAdapterTopics } from '../../utils/topics-utils.ts'
import { CONFIG_ADAPTER_WIDTH } from '../../utils/nodes-utils.ts'
import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'
import { useContextMenu } from '../../hooks/useContextMenu.tsx'
import { TopicFilter } from '../../types.ts'

const NodeAdapter: FC<NodeProps<Adapter>> = ({ id, data: adapter, selected }) => {
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol = protocols?.items?.find((e) => e.id === adapter.type)
  const { options } = useEdgeFlowContext()
  const topics = useMemo<TopicFilter[]>(() => {
    if (!adapterProtocol) return []
    if (!adapter.config) return []
    return discoverAdapterTopics(adapterProtocol, adapter.config).map((e) => ({ topic: e }))
  }, [adapter.config, adapterProtocol])
  const { onContextMenu } = useContextMenu(id, selected, '/edge-flow/node')

  return (
    <>
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        w={CONFIG_ADAPTER_WIDTH}
        p={2}
      >
        <VStack>
          <HStack w={'100%'}>
            <Image aria-label={adapter.type} boxSize="20px" objectFit="scale-down" src={adapterProtocol?.logoUrl} />
            <Text flex={1} data-testid={'adapter-node-name'}>
              {adapter.id}
            </Text>
          </HStack>
          {options.showStatus && (
            <Box flex={1}>
              <ConnectionStatusBadge status={adapter.status} />
            </Box>
          )}
          {options.showTopics && <TopicsContainer topics={topics} />}
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
    </>
  )
}

export default NodeAdapter
