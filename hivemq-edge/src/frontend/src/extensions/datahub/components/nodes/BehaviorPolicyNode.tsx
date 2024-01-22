import { CSSProperties, FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { BehaviorPolicyData, DataHubNodeType } from '../../types.ts'
import { styleSourceHandle } from '../../utils/node.utils.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'

const styleDeserializationHandle: CSSProperties = { width: '12px', top: '-6px', height: '12px' }

export const BehaviorPolicyNode: FC<NodeProps<BehaviorPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, type, data } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.BEHAVIOR_POLICY}/${id}`} {...props} wrapperProps={{ pt: 2 }}>
        <VStack>
          <HStack w={'100%'} justifyContent={'space-around'} pb={2}>
            <Text fontSize={'xs'}>
              {t('workspace.handles.behavior', { context: BehaviorPolicyData.Handle.SERIAL_PUBLISH })}
            </Text>
            <Text fontSize={'xs'}>
              {t('workspace.handles.behavior', { context: BehaviorPolicyData.Handle.SERIAL_WILL })}
            </Text>
          </HStack>
          <HStack>
            <NodeIcon type={DataHubNodeType.BEHAVIOR_POLICY} />
            <Text w={'45%'}> {t('workspace.nodes.type', { context: type })}</Text>
            <VStack>
              <Text>{data.model || '< none >'}</Text>
            </VStack>
          </HStack>
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Left} id={BehaviorPolicyData.Handle.CLIENT_FILTER} />
      <Handle
        type="target"
        position={Position.Top}
        id={BehaviorPolicyData.Handle.SERIAL_PUBLISH}
        style={{
          left: '30%',
          background: 'green',
          ...styleDeserializationHandle,
        }}
      />
      <Handle
        type="target"
        position={Position.Top}
        id={BehaviorPolicyData.Handle.SERIAL_WILL}
        style={{
          left: '76%',
          background: 'red',
          ...styleDeserializationHandle,
        }}
      />
      <Handle type="source" position={Position.Right} id="transitions" style={styleSourceHandle} />
    </>
  )
}
