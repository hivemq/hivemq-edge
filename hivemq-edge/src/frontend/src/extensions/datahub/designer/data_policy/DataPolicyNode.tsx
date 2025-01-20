import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, DataPolicyData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import PolicyToolbar from '@datahub/components/toolbar/PolicyToolbar.tsx'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

export const DataPolicyNode: FC<NodeProps<DataPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.DATA_POLICY}/${id}`} {...props} toolbar={<PolicyToolbar />}>
        <HStack justifyContent="space-between">
          <VStack alignItems="flex-start" data-testid="node-model">
            <Text fontSize="xs" h={6} alignContent="center">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.TOPIC_FILTER })}
            </Text>
            <Text fontSize="xs" h={6} alignContent="center">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.VALIDATION })}
            </Text>
          </VStack>
          <VStack alignItems="flex-end" data-testid="node-model">
            <Text fontSize="xs" h={6} alignContent="center">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.ON_SUCCESS })}
            </Text>
            <Text fontSize="xs" h={6} alignContent="center">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.ON_ERROR })}
            </Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle
        type="target"
        position={Position.Left}
        id={DataPolicyData.Handle.TOPIC_FILTER}
        style={{
          top: getHandlePosition(),
          borderColor: 'var(--chakra-colors-black)',
        }}
      />
      <CustomHandle
        type="target"
        position={Position.Left}
        id={DataPolicyData.Handle.VALIDATION}
        className={DataPolicyData.Handle.VALIDATION}
        style={{
          top: getHandlePosition(1),
          background: 'var(--chakra-colors-white)',
          borderColor: 'var(--chakra-colors-black)',
          borderWidth: 2,
        }}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_SUCCESS}
        className={DataPolicyData.Handle.ON_SUCCESS}
        style={{
          top: getHandlePosition(),
        }}
        isConnectable={1}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_ERROR}
        className={DataPolicyData.Handle.ON_ERROR}
        style={{
          top: getHandlePosition(1),
        }}
        isConnectable={1}
      />
    </>
  )
}
