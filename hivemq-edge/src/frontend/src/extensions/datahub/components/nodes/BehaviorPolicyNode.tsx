import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { BehaviorPolicyData, DataHubNodeType } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

export const BehaviorPolicyNode: FC<NodeProps<BehaviorPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, type, data } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.BEHAVIOR_POLICY}/${id}`} {...props}>
        <VStack>
          <HStack>
            <NodeIcon type={DataHubNodeType.BEHAVIOR_POLICY} />
            <Text w={'45%'}> {t('workspace.nodes.type', { context: type })}</Text>
            <VStack>
              <Text>{data.model || '< none >'}</Text>
            </VStack>
          </HStack>
        </VStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id={BehaviorPolicyData.Handle.CLIENT_FILTER} />
      <CustomHandle type="source" position={Position.Right} id="transitions" />
    </>
  )
}
