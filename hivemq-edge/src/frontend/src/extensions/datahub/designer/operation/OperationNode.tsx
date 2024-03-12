import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, OperationData } from '@datahub/types.ts'
import { NodeIcon } from '@datahub/components/helpers'
import { NodeWrapper } from '@datahub/components/nodes/NodeWrapper.tsx'
import NodeParams from '@datahub/components/helpers/NodeParams.tsx'
import { CustomHandle } from '@datahub/components/nodes/CustomHandle.tsx'

export const OperationNode: FC<NodeProps<OperationData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id, type } = props
  const { functionId, metadata } = data

  const isTransform = metadata?.hasArguments && data.functionId === 'DataHub.transform'
  const isSerialiser =
    metadata?.hasArguments &&
    (data.functionId === OperationData.Function.SERDES_SERIALIZE ||
      data.functionId === OperationData.Function.SERDES_DESERIALIZE)

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.OPERATION}/${id}`} {...props}>
        <VStack>
          <HStack w="100%" justifyContent="space-around">
            {isSerialiser && <Text fontSize="xs">{OperationData.Handle.SCHEMA}</Text>}
            {isTransform && (
              <>
                <Text fontSize="xs">{OperationData.Handle.DESERIALISER}</Text>
                <Text fontSize="xs">{OperationData.Handle.FUNCTION}</Text>
                <Text fontSize="xs">{OperationData.Handle.SERIALISER}</Text>
              </>
            )}
          </HStack>

          <HStack>
            <NodeIcon type={DataHubNodeType.OPERATION} />
            <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
            <VStack data-testid="node-model">
              <NodeParams value={functionId || t('error.noSet.select')} />
            </VStack>
          </HStack>
        </VStack>
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
      {isSerialiser && <CustomHandle type="target" position={Position.Top} id={OperationData.Handle.SCHEMA} />}
      {isTransform && (
        <>
          <CustomHandle
            type="target"
            position={Position.Top}
            id={OperationData.Handle.DESERIALISER}
            style={{ left: '20%' }}
          />
          <CustomHandle type="target" position={Position.Top} id={OperationData.Handle.FUNCTION} />
          <CustomHandle
            type="target"
            position={Position.Top}
            id={OperationData.Handle.SERIALISER}
            style={{ left: '80%' }}
          />
        </>
      )}
    </>
  )
}
