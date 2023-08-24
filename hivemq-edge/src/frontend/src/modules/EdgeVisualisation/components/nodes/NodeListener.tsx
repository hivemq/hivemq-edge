import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Image } from '@chakra-ui/react'

import logoTCP from '@/assets/app/gateway-tcp.svg'
import logoUDP from '@/assets/app/gateway-udp.svg'
import logoGateway from '@/assets/app/gateway.svg'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import { Listener } from '@/api/__generated__'
import { useTranslation } from 'react-i18next'

const NodeListener: FC<NodeProps<Listener>> = (props) => {
  const { t } = useTranslation()
  const { selected, data } = props

  const getLogo = () => {
    if (data.transport === Listener.transport.TCP) return logoTCP
    if (data.transport === Listener.transport.UDP) return logoUDP
    return logoGateway
  }

  return (
    <>
      <NodeWrapper p={2} borderRadius={60} backgroundColor={selected ? '#dddfe2' : 'white'} alignContent={'center'}>
        <Image src={getLogo()} alt={t('workspace.node.gateway') as string} boxSize="48px" />
      </NodeWrapper>
      <Handle type="target" position={Position.Right} id="Listeners" isConnectable={false} />
    </>
  )
}

export default NodeListener
