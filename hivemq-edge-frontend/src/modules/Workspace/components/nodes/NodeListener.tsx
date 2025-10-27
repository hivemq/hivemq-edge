import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useNodeConnections, useNodesData, useReactFlow } from '@xyflow/react'
import { Image } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import logoTCP from '@/assets/app/gateway-tcp.svg'
import logoUDP from '@/assets/app/gateway-udp.svg'
import logoGateway from '@/assets/app/gateway.svg'
import { Listener } from '@/api/__generated__'
import type { NodeListenerType } from '../../types'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'

import NodeWrapper from '../parts/NodeWrapper.tsx'

const NodeListener: FC<NodeProps<NodeListenerType>> = ({ id, selected, data }) => {
  const { t } = useTranslation()
  const { updateNodeData } = useReactFlow()

  // Use React Flow's efficient hooks to get connected nodes (edge node)
  const connections = useNodeConnections({ id })
  const connectedNodes = useNodesData(connections.map((connection) => connection.source))

  // Compute unified status model - derives from upstream edge node using React Flow's optimized hooks
  const statusModel = useMemo(() => {
    // Listener is always operational (represents configured MQTT listener)
    const operational = OperationalStatus.ACTIVE

    // Derive runtime status from upstream edge node
    if (!connectedNodes || connectedNodes.length === 0) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    // Get status from edge node (should only be one)
    const edgeNode = connectedNodes[0]
    if (!edgeNode) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    const edgeStatusModel = (edgeNode.data as { statusModel?: NodeStatusModel }).statusModel
    const runtime = edgeStatusModel?.runtime || RuntimeStatus.INACTIVE

    return {
      runtime,
      operational,
      source: 'DERIVED' as const,
    }
  }, [connectedNodes])

  // Update node data with statusModel whenever it changes
  useEffect(() => {
    updateNodeData(id, { statusModel })
  }, [id, statusModel, updateNodeData])

  const getLogo = () => {
    if (data.transport === Listener.transport.TCP) return logoTCP
    if (data.transport === Listener.transport.UDP) return logoUDP
    return logoGateway
  }

  return (
    <>
      <NodeWrapper
        isSelected={selected}
        statusModel={statusModel}
        p={2}
        borderRadius={60}
        backgroundColor={selected ? '#dddfe2' : 'white'}
        alignContent="center"
        sx={{
          _dark: {
            backgroundColor: selected ? '#dddfe2' : 'lightslategrey',
          },
        }}
      >
        <Image src={getLogo()} alt={t('workspace.node.gateway')} boxSize="48px" />
      </NodeWrapper>
      <Handle type="target" position={Position.Right} id="Listeners" isConnectable={false} />
    </>
  )
}

export default NodeListener
