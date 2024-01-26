import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, FunctionDefinition, OperationData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from '@/extensions/datahub/components/nodes/CustomHandle.tsx'

export const OperationNode: FC<NodeProps<OperationData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id, type } = props
  const { action } = data

  const model = useMemo(() => {
    if (!action) return undefined

    return typeof action === 'string'
      ? ({
          functionId: action,
        } as FunctionDefinition)
      : action
  }, [action])

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.OPERATION}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.OPERATION} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack>
            <Text data-testid="node-model">{model?.functionId || t('error.noSet.select')}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id={OperationData.Handle.INPUT} />
      {!model?.isTerminal && (
        <CustomHandle type="source" position={Position.Right} id={OperationData.Handle.OUTPUT} isConnectable={1} />
      )}
      {model?.hasArguments && <CustomHandle type="target" position={Position.Top} id={OperationData.Handle.SCHEMA} />}
    </>
  )
}
