import { FC, useMemo } from 'react'
import { ConnectionLineComponentProps, getSimpleBezierPath } from 'reactflow'
import { Tag } from '@chakra-ui/react'

import { getConnectedNodeFrom } from '@datahub/utils/node.utils.ts'
import { NodeIcon } from '@datahub/components/helpers'

const ConnectionLine: FC<ConnectionLineComponentProps> = ({
  fromHandle,
  fromNode,
  fromX,
  fromY,
  toY,
  toX,
  ...props
}) => {
  const droppedNode = useMemo(() => {
    return getConnectedNodeFrom(fromNode?.type, fromHandle?.id)
  }, [fromHandle?.id, fromNode?.type])

  const pathParams = {
    sourceX: fromX,
    sourceY: fromY,
    sourcePosition: props.fromPosition,
    targetX: toX,
    targetY: toY,
    targetPosition: props.toPosition,
  }

  const [d] = getSimpleBezierPath(pathParams)

  return (
    <g>
      <path fill="none" stroke="grey" strokeWidth={1.5} className="animated" d={d} />
      {props.connectionStatus === null && (
        <foreignObject x={toX} y={toY - 20} width="50px" height="50px">
          <Tag size="lg" colorScheme="gray" borderRadius="full" variant="outline">
            <NodeIcon type={droppedNode?.type} />
          </Tag>
        </foreignObject>
      )}
    </g>
  )
}

export default ConnectionLine
