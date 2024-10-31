import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Icon, Image } from '@chakra-ui/react'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { TopicIcon } from '@/components/Icons/TopicIcon.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { useNavigate } from 'react-router-dom'

const NodeEdge: FC<NodeProps> = (props) => {
  const { t } = useTranslation()
  const { onContextMenu } = useContextMenu(props.id, props.selected, '/workspace/edge')
  const navigate = useNavigate()

  return (
    <>
      <ContextualToolbar id={props.id} dragging={props.dragging} onOpenPanel={onContextMenu}>
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
        variant="unstyled"
        borderWidth={0}
        px={2}
        py={0}
      >
        <Image data-testid="edge-node" src={logo} alt={t('workspace.node.edge')} boxSize="96px" />
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
