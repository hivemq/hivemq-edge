import type { FC } from 'react'
import { useMemo } from 'react'
import { VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import type { NodeProps } from '@xyflow/react'
import { Position } from '@xyflow/react'

import { DataHubNodeType, FsmState, TransitionData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeParams } from '@datahub/components/helpers'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

export const TransitionNode: FC<NodeProps<TransitionData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id } = props

  const className = useMemo(() => {
    if (data.type === FsmState.Type.SUCCESS) return TransitionData.Handle.ON_SUCCESS
    if (data.type === FsmState.Type.FAILED) return TransitionData.Handle.ON_ERROR
    return undefined
  }, [data.type])

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.TRANSITION}/${id}`} {...props}>
        <VStack data-testid="node-model" alignItems="flex-end">
          <NodeParams value={data.event || t('error.noSet.select')} />
        </VStack>
      </NodeWrapper>
      <CustomHandle
        type="target"
        position={Position.Left}
        id={TransitionData.Handle.BEHAVIOR_POLICY}
        style={{
          top: getHandlePosition(0),
        }}
      />
      <CustomHandle
        type="source"
        id={TransitionData.Handle.OPERATION}
        position={Position.Right}
        isConnectable={1}
        className={className}
        style={{
          top: getHandlePosition(0),
        }}
      />
    </>
  )
}
