import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'
import { Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, OperationData } from '@datahub/types.ts'
import { NodeWrapper } from '@datahub/components/nodes/NodeWrapper.tsx'
import NodeParams from '@datahub/components/helpers/NodeParams.tsx'
import { CustomHandle } from '@datahub/components/nodes/CustomHandle.tsx'

export const OperationNode: FC<NodeProps<OperationData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id } = props
  const { functionId, metadata } = data

  const isTransform = metadata?.hasArguments && data.functionId === 'DataHub.transform'
  const isSerialiser =
    metadata?.hasArguments &&
    (data.functionId === OperationData.Function.SERDES_SERIALIZE ||
      data.functionId === OperationData.Function.SERDES_DESERIALIZE)

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.OPERATION}/${id}`} {...props}>
        <VStack alignItems="flex-start">
          {/*<HStack w="100%" justifyContent="space-around">*/}
          {/*  {isSerialiser && (*/}
          {/*    <Text fontSize="xs">{t('workspace.handles.operation', { context: OperationData.Handle.SCHEMA })}</Text>*/}
          {/*  )}*/}
          {/*  {isTransform && (*/}
          {/*    <>*/}
          {/*      <Text fontSize="xs">*/}
          {/*        {t('workspace.handles.operation', { context: OperationData.Handle.DESERIALISER })}*/}
          {/*      </Text>*/}
          {/*      <Text fontSize="xs">*/}
          {/*        {t('workspace.handles.operation', { context: OperationData.Handle.FUNCTION })}*/}
          {/*      </Text>*/}
          {/*      <Text fontSize="xs">*/}
          {/*        {t('workspace.handles.operation', { context: OperationData.Handle.SERIALISER })}*/}
          {/*      </Text>*/}
          {/*    </>*/}
          {/*  )}*/}
          {/*</HStack>*/}

          <VStack data-testid="node-model" alignItems="flex-start">
            <NodeParams value={functionId || t('error.noSet.select')} />
            {isSerialiser && (
              <Text fontSize="xs">{t('workspace.handles.operation', { context: OperationData.Handle.SCHEMA })}</Text>
            )}
            {isTransform && (
              <>
                <Text fontSize="xs">
                  {t('workspace.handles.operation', { context: OperationData.Handle.DESERIALISER })}
                </Text>
                <Text fontSize="xs">
                  {t('workspace.handles.operation', { context: OperationData.Handle.FUNCTION })}
                </Text>
                <Text fontSize="xs">
                  {t('workspace.handles.operation', { context: OperationData.Handle.SERIALISER })}
                </Text>
              </>
            )}
          </VStack>
        </VStack>
      </NodeWrapper>
      <CustomHandle
        type="target"
        position={Position.Left}
        id={OperationData.Handle.INPUT}
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 44px)`,
          // background: 'green',
        }}
      />
      {!metadata?.isTerminal && (
        <CustomHandle
          type="source"
          position={Position.Right}
          id={OperationData.Handle.OUTPUT}
          isConnectable={1}
          style={{
            top: `calc(var(--chakra-space-3) + 12px + 44px)`,
            // background: 'green',
          }}
        />
      )}
      {isSerialiser && (
        <CustomHandle
          type="target"
          position={Position.Left}
          id={OperationData.Handle.SCHEMA}
          style={{
            top: `calc(var(--chakra-space-3) + 12px + 16px + 1rem + 44px)`,
          }}
        />
      )}
      {isTransform && (
        <>
          <CustomHandle
            type="target"
            position={Position.Left}
            id={OperationData.Handle.DESERIALISER}
            style={{
              top: `calc(var(--chakra-space-3) + 12px + 16px + 1rem + 44px)`,
              // background: 'green',
            }}
          />
          <CustomHandle
            type="target"
            position={Position.Left}
            id={OperationData.Handle.FUNCTION}
            style={{
              top: `calc(var(--chakra-space-3) + 12px + 16px + 1rem + 44px + 1.5rem)`,
            }}
          />
          <CustomHandle
            type="target"
            position={Position.Left}
            id={OperationData.Handle.SERIALISER}
            style={{
              top: `calc(var(--chakra-space-3) + 12px + 16px + 1rem + 44px + 3rem)`,
            }}
          />
        </>
      )}
    </>
  )
}
