import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image, Text } from '@chakra-ui/react'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'
import GenericNode from './GenericNode.tsx'

const NodeEdge: FC<NodeProps> = (props) => {
  const { selected, data } = props

  return (
    <>
      <GenericNode backgroundColor={selected ? '#dddfe2' : undefined} alignContent={'center'}>
        <Text>{data.label}</Text>
        <Image src={logo} alt={'Edge'} boxSize="48px" />
      </GenericNode>
      <Handle type="target" position={Position.Bottom} id="Bottom" isConnectable={false} />
      <Handle type="target" position={Position.Top} id="Top" isConnectable={false} />
      <Handle type="target" position={Position.Left} id="Left" isConnectable={true} />
      <Handle type="target" position={Position.Right} id="Right" isConnectable={true} />
    </>
  )
}

export default NodeEdge
