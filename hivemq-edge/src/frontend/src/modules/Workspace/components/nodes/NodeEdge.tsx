import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'
import NodeWrapper from '../parts/NodeWrapper.tsx'

const NodeEdge: FC<NodeProps> = (props) => {
  const { t } = useTranslation()
  const { selected, data } = props

  return (
    <>
      <NodeWrapper
        isSelected={props.selected}
        backgroundColor={selected ? '#dddfe2' : undefined}
        alignContent={'center'}
      >
        <Text data-testid={'edge-node-name'}>{data.label}</Text>
        <Image src={logo} alt={t('workspace.node.edge') as string} boxSize="48px" />
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} id="Bottom" isConnectable={false} />
      <Handle type="target" position={Position.Top} id="Top" isConnectable={false} />
      <Handle type="source" position={Position.Left} id="SRight" isConnectable={false} />
    </>
  )
}

export default NodeEdge
