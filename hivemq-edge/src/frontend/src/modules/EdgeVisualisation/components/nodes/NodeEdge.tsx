import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image, Text } from '@chakra-ui/react'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'
import NodeWrapper from '../parts/NodeWrapper.tsx'

const NodeEdge: FC<NodeProps> = (props) => {
  const { selected, data } = props

  return (
    <>
      <NodeWrapper backgroundColor={selected ? '#dddfe2' : undefined} alignContent={'center'}>
        <Text>{data.label}</Text>
        <Image src={logo} alt={'Edge'} boxSize="48px" />
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} id="Bottom" isConnectable={false} />
      <Handle type="target" position={Position.Top} id="Top" isConnectable={false} />
      <Handle type="source" position={Position.Left} id="SRight" isConnectable={true} />
    </>
  )
}

export default NodeEdge
