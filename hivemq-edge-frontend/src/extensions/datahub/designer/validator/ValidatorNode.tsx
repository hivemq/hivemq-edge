import type { FC } from 'react'
import { HStack, VStack } from '@chakra-ui/react'
import type { NodeProps, Node } from '@xyflow/react'
import { Position } from '@xyflow/react'

import type { ValidatorData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeParams } from '@datahub/components/helpers'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

export const ValidatorNode: FC<NodeProps<Node<ValidatorData>>> = (props) => {
  const { id, data } = props
  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.VALIDATOR}/${id}`} {...props}>
        <HStack>
          <VStack data-testid="node-model">
            <NodeParams value={data.type} />
            <NodeParams value={data.strategy} />
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle
        type="target"
        position={Position.Left}
        id="target"
        style={{
          top: getHandlePosition(0),
        }}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id="source"
        style={{
          top: getHandlePosition(0),
        }}
      />
    </>
  )
}
