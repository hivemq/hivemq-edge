import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, DataPolicyData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

export const DataPolicyNode: FC<NodeProps<DataPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.DATA_POLICY}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.DATA_POLICY} />
          <Text> {t('workspace.nodes.type', { context: type })}</Text>
        </HStack>
        <VStack ml={6} alignItems={'flex-end'}>
          <Text fontSize={'xs'}>
            {t('workspace.handles.validation', { context: DataPolicyData.Handle.ON_SUCCESS })}
          </Text>
          <Text fontSize={'xs'}>{t('workspace.handles.validation', { context: DataPolicyData.Handle.ON_ERROR })}</Text>
        </VStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id={DataPolicyData.Handle.TOPIC_FILTER} />
      <CustomHandle type="target" position={Position.Top} id={DataPolicyData.Handle.VALIDATION} />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_SUCCESS}
        className={DataPolicyData.Handle.ON_SUCCESS}
        style={{
          top: `calc(var(--chakra-space-3) + 10px)`,
          // background: 'green',
        }}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_ERROR}
        className={DataPolicyData.Handle.ON_ERROR}
        style={{
          top: `calc(var(--chakra-space-3) + 10px + 16px + 0.5rem)`,
        }}
      />
    </>
  )
}
