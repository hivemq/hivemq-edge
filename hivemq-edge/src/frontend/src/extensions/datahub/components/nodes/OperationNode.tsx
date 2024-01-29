import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, OperationData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import NodeParams from '../helpers/NodeParams.tsx'
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
          <VStack data-testid="node-model">
            <NodeParams value={functionId || t('error.noSet.select')} />
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id={OperationData.Handle.INPUT} />
      {!metadata?.isTerminal && (
        <CustomHandle
          type="source"
          position={Position.Right}
          id={OperationData.Handle.OUTPUT}
          // TODO[18935] bug with the isConnectable routine
          // isConnectable={1}
        />
      )}
      {metadata?.hasArguments && (
        <CustomHandle type="target" position={Position.Top} id={OperationData.Handle.SCHEMA} />
      )}
    </>
  )
}
