import type { FC } from 'react'
import type { NodeProps, Node } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import type { CardBodyProps } from '@chakra-ui/react'
import { Card, CardBody, Text } from '@chakra-ui/react'

import type { FsmState } from '@datahub/types.ts'

const stateStyle: Record<string, CardBodyProps> = {
  INITIAL: { borderStyle: 'dashed', borderWidth: 2 },
  SUCCESS: { borderStyle: 'solid', borderWidth: 4 },
  INTERMEDIATE: {},
  FAILED: { borderStyle: 'dashed', borderWidth: 4 },
}

export const StateNode: FC<NodeProps<Node<FsmState>>> = (props) => {
  return (
    <>
      {props.data.type !== 'INITIAL' && (
        <Handle type="target" position={Position.Top} isConnectable={props.isConnectable} />
      )}
      <Card size="sm">
        <CardBody {...stateStyle[props.data.type]}>
          <Text>{props.data.name}</Text>
        </CardBody>
      </Card>
      {props.data.type !== 'SUCCESS' && props.data.type !== 'FAILED' && (
        <Handle type="source" position={Position.Bottom} isConnectable={props.isConnectable} />
      )}
    </>
  )
}
