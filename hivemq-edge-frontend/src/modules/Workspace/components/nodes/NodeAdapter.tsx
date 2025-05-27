import type { FC } from 'react'
import { useMemo } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useStore } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Box, HStack, Icon, Image, SkeletonText, Text, VStack } from '@chakra-ui/react'

import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useListNorthboundMappings.ts'
import { useListSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useListSouthboundMappings.ts'

import { SelectEntityType } from '@/components/MQTT/types'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import { deviceCapabilityIcon, isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'
import type { NodeAdapterType } from '@/modules/Workspace/types'

const NodeAdapter: FC<NodeProps<NodeAdapterType>> = ({ id, data: adapter, selected, dragging }) => {
  const { t } = useTranslation()
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol = protocols?.items?.find((e) => e.id === adapter.type)
  const { options } = useEdgeFlowContext()
  const { data: northMappings } = useListNorthboundMappings(adapter.id)
  const { data: southMappings } = useListSouthboundMappings(adapter.id)

  const northFlags = useMemo(() => {
    return northMappings?.items?.map((mapping) => mapping.topic) || []
  }, [northMappings?.items])

  const southFlags = useMemo(() => {
    const sss = southMappings?.items?.map((mapping) => mapping.topicFilter) || []
    return sss.filter((item) => item) as string[]
  }, [southMappings?.items])

  const { onContextMenu } = useContextMenu(id, selected, `/workspace/node/adapter/${adapter.type}`)
  const navigate = useNavigate()
  const showSkeleton = useStore(selectorIsSkeletonZoom)

  const bidirectional = isBidirectional(adapterProtocol)
  const adapterNavPath = `/workspace/node/adapter/${adapter.type}/${id}`

  return (
    <>
      <ContextualToolbar id={id} title={adapter.id} dragging={dragging} onOpenPanel={onContextMenu}>
        <ToolbarButtonGroup>
          <IconButton
            icon={<Icon as={deviceCapabilityIcon['READ']} />}
            data-testid="node-adapter-toolbar-northbound"
            aria-label={t('workspace.toolbar.command.mappings.northbound')}
            onClick={() => navigate(`${adapterNavPath}/northbound`)}
          />
          {bidirectional && (
            <IconButton
              icon={<Icon as={deviceCapabilityIcon['WRITE']} />}
              data-testid="node-adapter-toolbar-southbound"
              aria-label={t('workspace.toolbar.command.mappings.southbound')}
              onClick={() => navigate(`${adapterNavPath}/southbound`)}
            />
          )}
        </ToolbarButtonGroup>
      </ContextualToolbar>
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        w={CONFIG_ADAPTER_WIDTH}
        p={2}
      >
        {!showSkeleton && (
          <VStack>
            {bidirectional && <MappingBadge destinations={southFlags} type={SelectEntityType.TOPIC} />}

            <HStack>
              <Image aria-label={adapter.type} boxSize="20px" objectFit="scale-down" src={adapterProtocol?.logoUrl} />
              <Text flex={1} data-testid="adapter-node-name">
                {adapter.id}
              </Text>
            </HStack>
            {options.showStatus && (
              <Box flex={1}>
                <ConnectionStatusBadge status={adapter.status} />
              </Box>
            )}
            {options.showTopics && <MappingBadge destinations={northFlags} type={SelectEntityType.TOPIC} />}
          </VStack>
        )}
        {showSkeleton && (
          <HStack px={6} my="4">
            <Box>
              <ConnectionStatusBadge status={adapter.status} skeleton />
            </Box>
            <Box w="100%">
              <SkeletonText
                noOfLines={northFlags.length ? 2 : 1}
                spacing="4"
                skeletonHeight="2"
                startColor="gray.500"
                endColor="gray.500"
                aria-label={adapter.id}
              />
            </Box>
          </HStack>
        )}
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
      <Handle type="source" position={Position.Top} id="Top" isConnectable={false} />
    </>
  )
}

export default NodeAdapter
