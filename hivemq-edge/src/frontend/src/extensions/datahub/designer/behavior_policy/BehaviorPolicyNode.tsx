import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { BehaviorPolicyData, DataHubNodeType } from '@datahub/types.ts'
import { NodeIcon, NodeParams } from '@datahub/components/helpers'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import PolicyToolbar from '@datahub/components/controls/PolicyToolbar.tsx'

export const BehaviorPolicyNode: FC<NodeProps<BehaviorPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, type, data } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.BEHAVIOR_POLICY}/${id}`} toolbar={<PolicyToolbar />} {...props}>
        <VStack>
          <HStack>
            <NodeIcon type={DataHubNodeType.BEHAVIOR_POLICY} />
            <Text data-testid="node-title" w="45%">
              {t('workspace.nodes.type', { context: type })}
            </Text>
            <VStack data-testid="node-model">
              <NodeParams value={data.model || t('error.noSet.select')} />
            </VStack>
          </HStack>
        </VStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id={BehaviorPolicyData.Handle.CLIENT_FILTER} />
      <CustomHandle type="source" position={Position.Right} id={BehaviorPolicyData.Handle.TRANSITIONS} />
    </>
  )
}
