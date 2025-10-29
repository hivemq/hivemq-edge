import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useStore, useReactFlow } from '@xyflow/react'
import { Box, HStack, Image, SkeletonText, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import logo from '@/assets/hivemq/05-icon-hivemq-bridge-extension.svg'

import { SelectEntityType } from '@/components/MQTT/types'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'
import { getBridgeTopics } from '@/modules/Workspace/utils/topics-utils.ts'
import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import type { NodeBridgeType } from '@/modules/Workspace/types'
import { createBridgeStatusModel } from '@/modules/Workspace/utils/status-mapping.utils.ts'
import { OperationalStatus } from '@/modules/Workspace/types/status.types.ts'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import MappingBadge from '../parts/MappingBadge.tsx'

const NodeBridge: FC<NodeProps<NodeBridgeType>> = ({ id, selected, data: bridge, dragging }) => {
  const { t } = useTranslation()
  const topics = getBridgeTopics(bridge)
  const { options } = useEdgeFlowContext()
  const { onContextMenu } = useContextMenu(id, selected, `/workspace/bridge/${id}`)
  const showSkeleton = useStore(selectorIsSkeletonZoom)
  const { updateNodeData } = useReactFlow()

  // Compute unified status model with operational status based on topic filters
  const statusModel = useMemo(() => {
    const hasTopics = topics.local.length > 0 || topics.remote.length > 0

    // Operational status: ACTIVE if has topic filters configured, INACTIVE otherwise
    const operational = hasTopics ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE

    return createBridgeStatusModel(bridge.status, operational)
  }, [bridge.status, topics.local.length, topics.remote.length])

  // Update node data with statusModel whenever it changes
  useEffect(() => {
    updateNodeData(id, { statusModel })
  }, [id, statusModel, updateNodeData])

  return (
    <>
      <ContextualToolbar id={id} title={bridge.id} dragging={dragging} onOpenPanel={onContextMenu} />
      <NodeWrapper
        isSelected={selected}
        statusModel={statusModel}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
      >
        {!showSkeleton && (
          <VStack>
            {options.showTopics && (
              <MappingBadge destinations={topics.remote.map((filter) => filter.topic)} type={SelectEntityType.TOPIC} />
            )}
            <HStack>
              <Image boxSize="20px" objectFit="scale-down" src={logo} alt={t('workspace.node.bridge')} />
              <Text flex={1} data-testid="bridge-node-name">
                {bridge.id}
              </Text>
            </HStack>
            {options.showStatus && (
              <Box flex={1}>
                <ConnectionStatusBadge status={bridge.status} />
              </Box>
            )}
            {options.showTopics && (
              <MappingBadge destinations={topics.local.map((filter) => filter.topic)} type={SelectEntityType.TOPIC} />
            )}
          </VStack>
        )}
        {showSkeleton && (
          <HStack px={6} my="4">
            <Box>
              <ConnectionStatusBadge status={bridge.status} skeleton />
            </Box>
            <Box w="100%">
              <SkeletonText
                noOfLines={[...topics.local, ...topics.remote].length ? 2 : 1}
                spacing="4"
                skeletonHeight="2"
                startColor="gray.500"
                endColor="gray.500"
                aria-label={bridge.id}
              />
            </Box>
          </HStack>
        )}
      </NodeWrapper>
      <Handle type="source" position={Position.Top} id="Top" isConnectable={false} />
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
    </>
  )
}

export default NodeBridge
