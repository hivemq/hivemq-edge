import { FC } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath } from 'reactflow'
import { Icon, IconButton, useColorMode } from '@chakra-ui/react'
import { BiBarChartSquare } from 'react-icons/bi'

import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.tsx'

const MonitoringEdge: FC<EdgeProps> = ({
  source,
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
  const { t } = useTranslation()
  const { options } = useEdgeFlowContext()
  const navigate = useNavigate()
  const { colorMode } = useColorMode()
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
              aria-label={t('workspace.observability.aria-label', { device: source })}
              variant={'outline'}
              icon={<Icon as={BiBarChartSquare} boxSize={6} />}
              backgroundColor={colorMode === 'light' ? 'white' : 'gray.800'}
              color={style?.stroke}
              onClick={() => navigate(`/edge-flow/link/${id}`)}
              borderRadius={25}
            />
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}

export default MonitoringEdge
