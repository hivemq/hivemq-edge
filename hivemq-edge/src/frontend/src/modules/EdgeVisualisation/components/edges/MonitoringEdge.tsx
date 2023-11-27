import { FC } from 'react'
import { useNavigate } from 'react-router-dom'
import { BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath } from 'reactflow'
import { IconButton } from '@chakra-ui/react'
import { BiBarChartSquare } from 'react-icons/bi'

import { useEdgeFlowContext } from '@/modules/EdgeVisualisation/hooks/useEdgeFlowContext.tsx'

const MonitoringEdge: FC<EdgeProps> = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  markerEnd,
  style,
}) => {
  const { options } = useEdgeFlowContext()
  const navigate = useNavigate()
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  })

  return (
    <>
      <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} />
      {options.showMonitoringOnEdge && (
        <EdgeLabelRenderer>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,

              // everything inside EdgeLabelRenderer has no pointer events by default
              // if you have an interactive element, set pointer-events: all
              pointerEvents: 'all',
            }}
            className="nodrag nopan"
          >
            <IconButton
              aria-label={'grpah'}
              size={'xs'}
              variant={'outline'}
              icon={<BiBarChartSquare />}
              backgroundColor={'white'}
              color={style?.stroke}
              className="edgebutton"
              onDoubleClick={() => navigate(`/edge-flow/link/${id}`)}
              borderRadius={'25px'}
            />
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}

export default MonitoringEdge
