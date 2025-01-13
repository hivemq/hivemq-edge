import { FC } from 'react'
import { HStack, VStack } from '@chakra-ui/react'
import { NodeProps, Position } from 'reactflow'

import { DataHubNodeType, ValidatorData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeParams } from '@datahub/components/helpers'

export const ValidatorNode: FC<NodeProps<ValidatorData>> = (props) => {
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
          top: `calc(100% - 44px)`,
        }}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id="source"
        style={{
          top: `calc(100% - 44px)`,
        }}
      />
    </>
  )
}
