import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useStore, useReactFlow, useNodeConnections } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Box, HStack, Icon, Image, SkeletonText, Text, VStack } from '@chakra-ui/react'

import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListNorthboundMappings } from '@/api/hooks/useProtocolAdapters/useListNorthboundMappings.ts'
import { useListSouthboundMappings } from '@/api/hooks/useProtocolAdapters/useListSouthboundMappings.ts'
import { Status } from '@/api/__generated__'

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
import { NodeTypes } from '@/modules/Workspace/types'
import { createAdapterStatusModel } from '@/modules/Workspace/utils/status-mapping.utils.ts'
import { OperationalStatus } from '@/modules/Workspace/types/status.types.ts'

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

  // Compute unified status model with operational status based on mappings
  const statusModel = useMemo(() => {
    const hasNorthMappings = (northMappings?.items?.length ?? 0) > 0
    const hasSouthMappings = (southMappings?.items?.length ?? 0) > 0
    const isBidirectionalAdapter = isBidirectional(adapterProtocol)

    // Operational status: ACTIVE if has required mappings, INACTIVE otherwise
    const operational =
      hasNorthMappings && (!isBidirectionalAdapter || hasSouthMappings)
        ? OperationalStatus.ACTIVE
        : OperationalStatus.INACTIVE

    return createAdapterStatusModel(adapter.status, operational)
  }, [adapter.status, northMappings?.items, southMappings?.items, adapterProtocol])

  const { onContextMenu } = useContextMenu(id, selected, `/workspace/adapter/${adapter.type}/${id}`)
  const navigate = useNavigate()
  const showSkeleton = useStore(selectorIsSkeletonZoom)
  const { updateNodeData, setEdges, getNode } = useReactFlow()

  // Get outbound connections to update edge operational status
  const outboundConnections = useNodeConnections({ handleType: 'source', id })

  // Update node data with statusModel whenever it changes
  useEffect(() => {
    updateNodeData(id, { statusModel })
  }, [id, statusModel, updateNodeData])

  // Update edge operational status based on mapping types (Bug #2 fix)
  useEffect(() => {
    const hasNorthMappings = (northMappings?.items?.length ?? 0) > 0
    const hasSouthMappings = (southMappings?.items?.length ?? 0) > 0
    const isConnected =
      adapter.status?.connection === Status.connection.CONNECTED ||
      (adapter.status?.runtime === Status.runtime.STARTED && adapter.status?.connection === Status.connection.STATELESS)

    setEdges((edges) =>
      edges.map((edge) => {
        // Only update edges originating from this adapter
        if (edge.source !== id) return edge

        // Determine edge type by checking target node
        const targetNode = getNode(edge.target)
        if (!targetNode) return edge

        // Different operational status based on connection type:
        // - ADAPTER → EDGE (northbound): operational if has northbound mappings
        // - ADAPTER → DEVICE (southbound): operational if has southbound mappings
        // - ADAPTER → MAPPER/COMBINER: keep INACTIVE for now (TODO: future work)
        let shouldAnimate = false

        if (targetNode.type === NodeTypes.EDGE_NODE) {
          // Northbound connection: check northbound mappings
          shouldAnimate = isConnected && hasNorthMappings
        } else if (targetNode.type === NodeTypes.DEVICE_NODE) {
          // Southbound connection: check southbound mappings
          shouldAnimate = isConnected && hasSouthMappings
        }
        // For MAPPER, COMBINER, etc.: leave shouldAnimate as false (INACTIVE)

        // Only update if animation state changed
        if (edge.animated === shouldAnimate) return edge

        return {
          ...edge,
          animated: shouldAnimate,
        }
      })
    )
  }, [
    id,
    adapter.status,
    northMappings?.items?.length,
    southMappings?.items?.length,
    outboundConnections,
    setEdges,
    getNode,
  ])

  const bidirectional = isBidirectional(adapterProtocol)
  const adapterNavPath = `/workspace/adapter/${adapter.type}/${id}`

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
        statusModel={statusModel}
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
