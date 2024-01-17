import { HStack, Text, VStack } from '@chakra-ui/react'
import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'

import { DataHubNodeType, SchemaData } from '../../types.ts'
import { styleSourceHandle } from '../../utils/node.utils.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'

export const SchemaNode: FC<NodeProps<SchemaData>> = (props) => {
  const { id, data } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.SCHEMA}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.SCHEMA} />
          <Text>Schema</Text>
          <VStack>
            <Text>{data?.type}</Text>
            <Text>{data?.schemaSource?.title || '< not defined>'}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} style={styleSourceHandle} />
    </>
  )
}
