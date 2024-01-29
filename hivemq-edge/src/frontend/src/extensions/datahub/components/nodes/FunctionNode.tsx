import { Code, HStack, Text, VStack } from '@chakra-ui/react'
import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'

import { DataHubNodeType, FunctionData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'
import { useTranslation } from 'react-i18next'

export const FunctionNode: FC<NodeProps<FunctionData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.FUNCTION}/${id}`} {...props}>
        <VStack>
          {import.meta.env.VITE_FLAG_DATAHUB_TRANSFORM_EXTENDED === 'true' && (
            <HStack w="100%" justifyContent="space-around">
              <Text fontSize="xs">{FunctionData.Handle.SERIALISER}</Text>
              <Text fontSize="xs">{FunctionData.Handle.DESERIALISER}</Text>
            </HStack>
          )}
          <HStack>
            <NodeIcon type={DataHubNodeType.FUNCTION} />
            <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
            <VStack data-testid="node-model">
              <Code>{data?.name || t('error.noSet.select')}</Code>
            </VStack>
          </HStack>
        </VStack>
      </NodeWrapper>
      <CustomHandle type="source" position={Position.Bottom} id="source" />
      {import.meta.env.VITE_FLAG_DATAHUB_TRANSFORM_EXTENDED === 'true' && (
        <>
          <CustomHandle
            type="target"
            position={Position.Top}
            id={FunctionData.Handle.SERIALISER}
            style={{ left: '25%' }}
          />
          <CustomHandle
            type="target"
            position={Position.Top}
            id={FunctionData.Handle.DESERIALISER}
            style={{ left: '75%' }}
          />
        </>
      )}
    </>
  )
}
