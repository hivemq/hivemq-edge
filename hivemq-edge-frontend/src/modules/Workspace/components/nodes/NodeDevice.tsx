import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useStore, useNodeConnections, useNodesData, useReactFlow } from '@xyflow/react'
import { HStack, Icon, Text, VStack } from '@chakra-ui/react'

import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import TooltipIcon from '@/components/Chakra/TooltipIcon.tsx'
import { PLCTagIcon } from '@/components/Icons/TopicIcon.tsx'
import { SelectEntityType } from '@/components/MQTT/types'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import type { NodeDeviceType } from '@/modules/Workspace/types.ts'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import {
  deviceCapabilityIcon,
  deviceCategoryIcon,
  ProtocolAdapterCategoryName,
} from '@/modules/Workspace/utils/adapter.utils.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'

const NodeDevice: FC<NodeProps<NodeDeviceType>> = ({ id, selected, data, dragging }) => {
  const { t } = useTranslation()
  const { onContextMenu } = useContextMenu(id, selected, `/workspace/device/${id}`)
  const { category, capabilities } = data
  const showSkeleton = useStore(selectorIsSkeletonZoom)
  const { data: deviceTags } = useGetDomainTags(data.sourceAdapterId)
  const { updateNodeData } = useReactFlow()

  // Use React Flow's efficient hooks to get connected nodes (parent adapter)
  const connections = useNodeConnections({ id })
  const connectedNodes = useNodesData(connections.map((connection) => connection.source))

  const tagNames = useMemo(() => {
    return deviceTags?.items?.map((tag) => tag.name) || []
  }, [deviceTags?.items])

  // Compute unified status model - derives from parent adapter using React Flow's optimized hooks
  const statusModel = useMemo(() => {
    const hasTags = tagNames.length > 0

    // Operational status: ACTIVE if has tags configured, INACTIVE otherwise
    const operational = hasTags ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE

    // Derive runtime status from parent adapter
    if (!connectedNodes || connectedNodes.length === 0) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    // Get status from parent adapter (should only be one)
    const parentAdapter = connectedNodes[0]
    if (!parentAdapter) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    const parentStatusModel = (parentAdapter.data as { statusModel?: NodeStatusModel }).statusModel
    const runtime = parentStatusModel?.runtime || RuntimeStatus.INACTIVE

    return {
      runtime,
      operational,
      source: 'DERIVED' as const,
    }
  }, [connectedNodes, tagNames.length])

  // Update node data with statusModel whenever it changes
  useEffect(() => {
    updateNodeData(id, { statusModel })
  }, [id, statusModel, updateNodeData])

  return (
    <>
      <ContextualToolbar id={id} title={data.protocol} onOpenPanel={onContextMenu} dragging={dragging} hasNoOverview>
        <ToolbarButtonGroup>
          <IconButton
            icon={<PLCTagIcon />}
            data-testid="node-device-toolbar-metadata"
            aria-label={t('workspace.toolbar.command.device.metadata')}
            onClick={onContextMenu}
          />
        </ToolbarButtonGroup>
      </ContextualToolbar>
      <NodeWrapper
        isSelected={selected}
        statusModel={statusModel}
        wordBreak="break-word"
        textAlign="center"
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
        borderTopRadius={30}
      >
        <VStack>
          {!showSkeleton && (
            <>
              <HStack w="100%" justifyContent="flex-end" gap={1} data-testid="device-capabilities">
                {capabilities?.map((capability) => (
                  <TooltipIcon
                    key={capability}
                    boxSize={4}
                    as={deviceCapabilityIcon[capability]}
                    data-type={capability}
                    aria-label={t('device.capability.description', { context: capability })}
                  />
                ))}
              </HStack>
              <HStack w="100%" data-testid="device-description">
                <Icon
                  as={deviceCategoryIcon[category?.name || ProtocolAdapterCategoryName.SIMULATION]}
                  data-type={category?.name}
                />
                <Text>{data.protocol}</Text>
              </HStack>
              <MappingBadge destinations={tagNames} type={SelectEntityType.TAG} />
            </>
          )}
          {showSkeleton && (
            <Icon
              as={deviceCategoryIcon[category?.name || ProtocolAdapterCategoryName.SIMULATION]}
              data-type={category?.name}
              boxSize="14"
            />
          )}
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NodeDevice
