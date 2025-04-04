import type { FC } from 'react'
import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
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
import { CONFIG_ADAPTER_WIDTH } from '../../utils/nodes-utils'
import MappingBadge from '../parts/MappingBadge'

const NodeEdge: FC<NodeProps> = (props) => {
  const { t } = useTranslation()
  const { onContextMenu } = useContextMenu(props.id, props.selected, '/workspace/edge')
  const navigate = useNavigate()
  const { data } = useListTopicFilters()

  const topicFilters = useMemo(() => {
    return data?.items.map((e) => e.topicFilter) || []
  }, [data?.items])

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
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        flexDirection="row"
        w={CONFIG_ADAPTER_WIDTH}
        p={2}
        h={120}
      >
        <VStack h={'100%'} justifyContent={'center'}>
          <Image data-testid="edge-node-icon" src={logo} alt={t('workspace.node.edge')} boxSize="96px" />
        </VStack>
        <VStack h={'100%'} flex={1} justifyContent={'space-between'}>
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
