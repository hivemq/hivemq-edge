import type { FC } from 'react'
import { useMemo } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position, useNodeConnections, useNodesData } from '@xyflow/react'
import { Text } from '@chakra-ui/react'

import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import type { NodeHostType } from '../../types'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'
import { useSyncNodeStatusModel } from '@/modules/Workspace/hooks/useSyncNodeStatusModel.ts'

const NodeHost: FC<NodeProps<NodeHostType>> = ({ id, selected, data }) => {
  const { label } = data

  // Use React Flow's efficient hooks to get connected nodes (parent bridge)
  const connections = useNodeConnections({ id })
  // useNodesData memoises its selector on the ids array, so a fresh array on every render makes
  // the store recompute and return a new selection every time.
  const connectedNodeIds = useMemo(() => connections.map((connection) => connection.source), [connections])
  const connectedNodes = useNodesData(connectedNodeIds)

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

  useSyncNodeStatusModel(id, statusModel, data.statusModel)
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
