import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, DataPolicyData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import PolicyToolbar from '@datahub/components/toolbar/PolicyToolbar.tsx'

export const DataPolicyNode: FC<NodeProps<DataPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.DATA_POLICY}/${id}`} {...props} toolbar={<PolicyToolbar />}>
        <HStack justifyContent="space-between">
          <VStack alignItems="flex-start" data-testid="node-model">
            <Text fontSize="xs">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.TOPIC_FILTER })}
            </Text>
            <Text fontSize="xs">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.VALIDATION })}
            </Text>
          </VStack>
          <VStack alignItems="flex-end" data-testid="node-model">
            <Text fontSize="xs">
              {t('workspace.handles.validation', { context: DataPolicyData.Handle.ON_SUCCESS })}
            </Text>
            <Text fontSize="xs">{t('workspace.handles.validation', { context: DataPolicyData.Handle.ON_ERROR })}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle
        type="target"
        position={Position.Left}
        id={DataPolicyData.Handle.TOPIC_FILTER}
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 44px)`,
          borderColor: 'var(--chakra-colors-black)',
        }}
      />
      <CustomHandle
        type="target"
        position={Position.Left}
        id={DataPolicyData.Handle.VALIDATION}
        className={DataPolicyData.Handle.VALIDATION}
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 16px + 0.5rem + 44px)`,
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
          top: `calc(var(--chakra-space-3) + 12px + 44px)`,
          // background: 'green',
        }}
        isConnectable={1}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_ERROR}
        className={DataPolicyData.Handle.ON_ERROR}
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 16px + 0.5rem + 44px)`,
        }}
        isConnectable={1}
      />
    </>
  )
}
