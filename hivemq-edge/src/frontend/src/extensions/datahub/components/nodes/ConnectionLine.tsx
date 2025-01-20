import { FC, useMemo } from 'react'
import { ConnectionLineComponentProps, getSmoothStepPath } from 'reactflow'
import { Tag } from '@chakra-ui/react'

import { getConnectedNodeFrom } from '@datahub/utils/node.utils.ts'
import { NodeIcon } from '@datahub/components/helpers'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DataHubNodeType } from '@datahub/types.ts'

const ICON_SIZE = 50
const ICON_OFFSET = 20

const ConnectionLine: FC<ConnectionLineComponentProps> = ({
  fromHandle,
  fromNode,
  fromX,
  fromY,
  toY,
  toX,
  ...props
}) => {
  const { isPolicyInDraft } = useDataHubDraftStore()
  const droppedNode = useMemo(() => {
    const droppedNode = getConnectedNodeFrom(fromNode?.type, fromHandle?.id)
    if (
      droppedNode?.type === DataHubNodeType.DATA_POLICY ||
      (droppedNode?.type === DataHubNodeType.BEHAVIOR_POLICY && isPolicyInDraft())
    )
      return undefined
    return droppedNode
  }, [fromHandle?.id, fromNode?.type, isPolicyInDraft])

  const pathParams = {
    sourceX: fromX,
    sourceY: fromY,
    sourcePosition: props.fromPosition,
    targetX: toX,
    targetY: toY,
    targetPosition: props.toPosition,
  }

  const [d] = getSmoothStepPath(pathParams)

  return (
    <g>
      <path fill="none" stroke="grey" strokeWidth={1.5} className="animated" d={d} />
      {props.connectionStatus === null && (
        <foreignObject
          x={toX - (props.fromPosition === 'left' ? ICON_SIZE : 0)}
          y={toY - ICON_OFFSET}
          width={`${ICON_SIZE}px`}
          height={`${ICON_SIZE}px`}
        >
          <Tag size="lg" colorScheme="gray" borderRadius="full" variant="outline">
            <NodeIcon type={droppedNode?.type} />
          </Tag>
        </foreignObject>
      )}
    </g>
  )
}

export default ConnectionLine
