import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'

const NodeEdge: FC<NodeProps> = () => {
  const { t } = useTranslation()

  return (
    <>
      <Image data-testid={'edge-node'} src={logo} alt={t('workspace.node.edge') as string} boxSize="96px" />
      <Handle type="target" position={Position.Bottom} id="Bottom" isConnectable={false} style={{ bottom: '1px' }} />
      <Handle type="target" position={Position.Top} id="Top" isConnectable={false} style={{ top: '1px' }} />
      <Handle
        type="source"
        position={Position.Left}
        id="SRight"
        isConnectable={false}
        style={{ top: '60%', left: '-1px' }}
      />
    </>
  )
}

export default NodeEdge
