import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { VStack } from '@chakra-ui/react'

import { BehaviorPolicyData, DataHubNodeType } from '@datahub/types.ts'
import { NodeParams } from '@datahub/components/helpers'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import PolicyToolbar from '@datahub/components/toolbar/PolicyToolbar.tsx'

export const BehaviorPolicyNode: FC<NodeProps<BehaviorPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.BEHAVIOR_POLICY}/${id}`} toolbar={<PolicyToolbar />} {...props}>
        <VStack data-testid="node-model" alignItems="flex-end">
          <NodeParams value={data.model || t('error.noSet.select')} />
        </VStack>
      </NodeWrapper>
      <CustomHandle
        type="target"
        position={Position.Left}
        id={BehaviorPolicyData.Handle.CLIENT_FILTER}
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 48px `,
        }}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={BehaviorPolicyData.Handle.TRANSITIONS}
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 48px `,
        }}
      />
    </>
  )
}
