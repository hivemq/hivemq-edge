import { FC, useMemo } from 'react'
import { BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath } from 'reactflow'

import DataPolicyEdgeCTA from '@/modules/Workspace/components/edges/DataPolicyEdgeCTA.tsx'
import ObservabilityEdgeCTA from '@/modules/Workspace/components/edges/ObservabilityEdgeCTA.tsx'
import { useGetPoliciesMatching } from '@/modules/Workspace/hooks/useGetPoliciesMatching.ts'

import { DataHubNodeType } from '@datahub/types.ts'
import { HStack } from '@chakra-ui/react'
import { useNavigate } from 'react-router-dom'

const MonitoringEdge: FC<EdgeProps> = (props) => {
  const { id, source, sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition, markerEnd, style } = props
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  })
  const policies = useGetPoliciesMatching(source)
  const navigate = useNavigate()

  const policyRoutes = useMemo(() => {
    if (!policies) return undefined

    return policies.map((e) => `/datahub/${DataHubNodeType.DATA_POLICY}/${e.id}`)
  }, [policies])

  const handleOpenObservability = () => {
    navigate(`/edge-flow/link/${id}`)
  }

  const handleShowPolicy = (route: string) => {
    navigate(route)
  }

  const handleShowAllPolicies = () => {
    navigate('/datahub')
  }

  return (
    <>
      <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} />
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
              style={style}
              policyRoutes={policyRoutes}
              onClickPolicy={handleShowPolicy}
              onClickAll={handleShowAllPolicies}
            />
          )}
        </HStack>
      </EdgeLabelRenderer>
    </>
  )
}

export default MonitoringEdge
