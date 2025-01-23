import type { FC } from 'react'
import type { EdgeProps } from 'reactflow'
import { BaseEdge, getSmoothStepPath } from 'reactflow'

import { styleDefaultEdge } from '@datahub/utils/edge.utils.ts'

const DataHubPolicyEdge: FC<EdgeProps> = (props) => {
  const { sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition, markerEnd, style } = props

  const [stepPath] = getSmoothStepPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  })

  const selectedStyle = props.selected && {
    filter: 'drop-shadow( 1px 1px 2px rgba(0, 0, 0, .5))',
  }

  const localStyle = {
    ...style,
    ...styleDefaultEdge.style,
    ...selectedStyle,
  }

  return <BaseEdge path={stepPath} markerEnd={markerEnd} style={localStyle} />
}

export default DataHubPolicyEdge
