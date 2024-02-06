import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { Card, CardBody, Text } from '@chakra-ui/react'

import { FsmState } from '@datahub/types.ts'

export const StateNode: FC<NodeProps<FsmState>> = (props) => {
  return (
    <>
      {props.data.type !== 'INITIAL' && (
        <Handle type="target" position={Position.Top} isConnectable={props.isConnectable} />
      )}
      <Card size="sm">
        <CardBody>
          <Text>{props.data.name}</Text>
        </CardBody>
      </Card>
      {props.data.type !== 'SUCCESS' && props.data.type !== 'FAILED' && (
        <Handle type="source" position={Position.Bottom} isConnectable={props.isConnectable} />
      )}
    </>
  )
}
