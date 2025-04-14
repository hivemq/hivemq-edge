import { HStack } from '@chakra-ui/react'
import type { FC } from 'react'
import { useMemo } from 'react'
import {
  BaseEdge,
  EdgeLabelRenderer,
  type EdgeProps,
  getBezierPath,
  useInternalNode,
  useNodesData,
} from '@xyflow/react'

import { getEdgeParams } from '@/modules/Workspace/utils/edge.utils'
import { useNavigate } from 'react-router-dom'
import { DataHubNodeType } from '../../../../extensions/datahub/types'
import { useGetPoliciesMatching } from '../../hooks/useGetPoliciesMatching'
import { NodeTypes } from '../../types'
import DataPolicyEdgeCTA from './DataPolicyEdgeCTA'
import ObservabilityEdgeCTA from './ObservabilityEdgeCTA'

export const DynamicEdge: FC<EdgeProps> = ({ id, source, target, markerEnd, style }) => {
  const sourceNode = useInternalNode(source)
  const targetNode = useInternalNode(target)
  const policies = useGetPoliciesMatching(source)
  const navigate = useNavigate()
  const nodeData = useNodesData(target)

  const isObservable = useMemo(() => {
    if (!nodeData) return false
    const { type } = nodeData
    return type === NodeTypes.EDGE_NODE
  }, [nodeData])

  const policyRoutes = useMemo(() => {
    if (!policies) return undefined

    return policies.map((policy) => `/datahub/${DataHubNodeType.DATA_POLICY}/${policy.id}`)
  }, [policies])

  const handleOpenObservability = () => {
    navigate(`/workspace/link/${id}`)
  }

  const handleShowPolicy = (route: string) => {
    navigate(route)
  }

  const handleShowAllPolicies = () => {
    navigate('/datahub')
  }

  if (!sourceNode || !targetNode) {
    return null
  }

  const { sx, sy, tx, ty, sourcePos, targetPos } = getEdgeParams(sourceNode, targetNode)

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  })

  return (
    <>
      <BaseEdge id={id} path={edgePath} markerEnd={markerEnd} style={style} />
      {isObservable && (
        <EdgeLabelRenderer>
          <HStack
            sx={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
              pointerEvents: 'all',
            }}
            className="nodrag nopan"
          >
            <ObservabilityEdgeCTA source={source} style={style} onClick={handleOpenObservability} />
            {policyRoutes && (
              <DataPolicyEdgeCTA
                data-testid="policy-panel-trigger"
                style={style}
                policyRoutes={policyRoutes}
                onClickPolicy={handleShowPolicy}
                onClickAll={handleShowAllPolicies}
              />
            )}
          </HStack>
        </EdgeLabelRenderer>
      )}
    </>
  )
}
