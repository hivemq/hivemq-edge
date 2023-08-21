import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image } from '@chakra-ui/react'

import logoTCP from '@/assets/app/gateway-tcp.svg'
import logoUDP from '@/assets/app/gateway-udp.svg'
import logoGateway from '@/assets/app/gateway.svg'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import { Listener } from '@/api/__generated__'

const NodeListener: FC<NodeProps<Listener>> = (props) => {
  const { selected, data } = props

  const getLogo = () => {
    if (data.description?.includes('TCP')) return logoTCP
    if (data.description?.includes('UDP')) return logoUDP
    return logoGateway
  }

  return (
    <>
      <NodeWrapper p={2} borderRadius={60} backgroundColor={selected ? '#dddfe2' : 'white'} alignContent={'center'}>
        <Image src={getLogo()} alt={'Edge'} boxSize="48px" />
      </NodeWrapper>
      <Handle type="target" position={Position.Right} id="Listeners" isConnectable={false} />
    </>
  )
}

export default NodeListener
