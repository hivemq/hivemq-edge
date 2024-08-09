import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'

const NodeEdge: FC<NodeProps> = (props) => {
  const { t } = useTranslation()
  const { onContextMenu } = useContextMenu(props.id, props.selected, '/workspace/edge')

  return (
    <>
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
      </NodeWrapper>
    </>
  )
}

export default NodeEdge
