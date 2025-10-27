import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useNodeConnections, useNodesData, useReactFlow } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Icon, Image, Text, VStack } from '@chakra-ui/react'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'

import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters'
import { SelectEntityType } from '@/components/MQTT/types'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { TopicIcon } from '@/components/Icons/TopicIcon.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import type { NodeEdgeType } from '../../types'
import { CONFIG_ADAPTER_WIDTH } from '../../utils/nodes-utils'
import MappingBadge from '../parts/MappingBadge'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'

const NodeEdge: FC<NodeProps<NodeEdgeType>> = (props) => {
  const { t } = useTranslation()
  const { onContextMenu } = useContextMenu(props.id, props.selected, `/workspace/edge/${props.id}`)
  const navigate = useNavigate()
  const { data } = useListTopicFilters()
  const { updateNodeData } = useReactFlow()

  // Use React Flow's efficient hooks to get connected nodes (adapters, bridges, pulse)
  const connections = useNodeConnections({ handleType: 'target', id: props.id })
  const connectedNodes = useNodesData(connections.map((connection) => connection.source))

  const topicFilters = useMemo(() => {
    return data?.items.map((e) => e.topicFilter) || []
  }, [data?.items])

  // Compute unified status model with operational status based on topic filters
  const statusModel = useMemo(() => {
    const hasTopicFilters = topicFilters.length > 0

    // Operational status: ACTIVE if has topic filters, INACTIVE otherwise
    const operational = hasTopicFilters ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE

    // Derive runtime status from upstream active nodes (adapters, bridges, pulse)
    if (!connectedNodes || connectedNodes.length === 0) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    let hasErrorUpstream = false
    let hasActiveUpstream = false

    for (const node of connectedNodes) {
      if (!node) continue
      const upstreamStatusModel = (node.data as { statusModel?: NodeStatusModel }).statusModel
      if (!upstreamStatusModel) continue

      if (upstreamStatusModel.runtime === RuntimeStatus.ERROR) {
        hasErrorUpstream = true
      } else if (upstreamStatusModel.runtime === RuntimeStatus.ACTIVE) {
        hasActiveUpstream = true
      }
    }

    // ERROR propagates first
    const runtime = hasErrorUpstream
      ? RuntimeStatus.ERROR
      : hasActiveUpstream
        ? RuntimeStatus.ACTIVE
        : RuntimeStatus.INACTIVE

    return {
      runtime,
      operational,
      source: 'DERIVED' as const,
    }
  }, [connectedNodes, topicFilters.length])

  // Update node data with statusModel whenever it changes
  useEffect(() => {
    updateNodeData(props.id, { statusModel })
  }, [props.id, statusModel, updateNodeData])

  return (
    <>
      <ContextualToolbar
        id={props.id}
        title={t('branding.appName')}
        dragging={props.dragging}
        onOpenPanel={onContextMenu}
      >
        <ToolbarButtonGroup>
          <IconButton
            icon={<Icon as={TopicIcon} />}
            aria-label={t('workspace.toolbar.command.edge.topicFilter')}
            onClick={() => navigate(`topic-filters/`)}
          />
        </ToolbarButtonGroup>
      </ContextualToolbar>
      <NodeWrapper
        isSelected={props.selected}
        statusModel={statusModel}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        flexDirection="row"
        w={CONFIG_ADAPTER_WIDTH}
        p={2}
        h={120}
      >
        <VStack h="100%" justifyContent="center">
          <Image data-testid="edge-node-icon" src={logo} alt={t('workspace.node.edge')} boxSize="96px" />
        </VStack>
        <VStack h="100%" flex={1} justifyContent="space-between">
          <Text data-testid="edge-node-title">{t('branding.appName')}</Text>
          <MappingBadge destinations={topicFilters} type={SelectEntityType.TOPIC_FILTER} />
        </VStack>
        .
        <Handle
          type="target"
          position={Position.Bottom}
          id="Bottom"
          isConnectable={false}
          style={{ bottom: '-3px', minWidth: 0, minHeight: 0, width: 0, height: 0 }}
        />
        <Handle
          type="target"
          position={Position.Top}
          id="Top"
          isConnectable={false}
          style={{ top: '-3px', minWidth: 0, minHeight: 0, width: 0, height: 0 }}
        />
        <Handle
          type="source"
          position={Position.Left}
          id="SRight"
          isConnectable={false}
          style={{ top: '60%', left: '-3px', minWidth: 0, minHeight: 0, width: 0, height: 0 }}
        />
        <Handle
          type="target"
          position={Position.Right}
          id="clientHandle"
          isConnectable={false}
          style={{ top: '60%', right: '-3px', minWidth: 0, minHeight: 0, width: 0, height: 0 }}
        />
      </NodeWrapper>
    </>
  )
}

export default NodeEdge
