import type { FC } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Position } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { VStack } from '@chakra-ui/react'

import { BehaviorPolicyData, DataHubNodeType } from '@datahub/types.ts'
import { NodeParams } from '@datahub/components/helpers'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import PolicyToolbar from '@datahub/components/toolbar/PolicyToolbar.tsx'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

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
          top: getHandlePosition(0),
        }}
      />
      <CustomHandle
        type="source"
        position={Position.Right}
        id={BehaviorPolicyData.Handle.TRANSITIONS}
        style={{
          top: getHandlePosition(0),
        }}
      />
    </>
  )
}
