import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useNodeConnections, useNodesData, useReactFlow } from '@xyflow/react'
import { Text } from '@chakra-ui/react'

import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import type { NodeHostType } from '../../types'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'

const NodeHost: FC<NodeProps<NodeHostType>> = ({ id, selected, data }) => {
  const { label } = data
  const { updateNodeData } = useReactFlow()

  // Use React Flow's efficient hooks to get connected nodes (parent bridge)
  const connections = useNodeConnections({ id })
  const connectedNodes = useNodesData(connections.map((connection) => connection.source))

  // Compute unified status model - derives from parent bridge using React Flow's optimized hooks
  const statusModel = useMemo(() => {
    // Host is always operational (represents external MQTT broker)
    const operational = OperationalStatus.ACTIVE

    // Derive runtime status from parent bridge
    if (!connectedNodes || connectedNodes.length === 0) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    // Get status from parent bridge (should only be one)
    const parentBridge = connectedNodes[0]
    if (!parentBridge) {
      return {
        runtime: RuntimeStatus.INACTIVE,
        operational,
        source: 'DERIVED' as const,
      }
    }

    const parentStatusModel = (parentBridge.data as { statusModel?: NodeStatusModel }).statusModel
    const runtime = parentStatusModel?.runtime || RuntimeStatus.INACTIVE

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
  return (
    <>
      <NodeWrapper
        isSelected={selected}
        statusModel={statusModel}
        wordBreak="break-word"
        textAlign="center"
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
        borderBottomRadius={30}
      >
        <Text pb={5}>{label}</Text>
      </NodeWrapper>
      <Handle type="target" position={Position.Top} isConnectable={false} />
    </>
  )
}

export default NodeHost
