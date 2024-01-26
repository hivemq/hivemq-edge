import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, OperationData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

export const OperationNode: FC<NodeProps<OperationData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id, type } = props
  const { functionId, metadata } = data

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.OPERATION}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.OPERATION} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack>
            <Text data-testid="node-model">{functionId || t('error.noSet.select')}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id={OperationData.Handle.INPUT} />
      {!metadata?.isTerminal && (
        <CustomHandle type="source" position={Position.Right} id={OperationData.Handle.OUTPUT} isConnectable={1} />
      )}
      {metadata?.hasArguments && (
        <CustomHandle type="target" position={Position.Top} id={OperationData.Handle.SCHEMA} />
      )}
    </>
  )
}
