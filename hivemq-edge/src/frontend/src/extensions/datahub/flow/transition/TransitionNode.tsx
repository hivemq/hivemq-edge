import { FC } from 'react'
import { HStack, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'

import { DataHubNodeType, TransitionData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeIcon, NodeParams } from '@datahub/components/helpers'

export const TransitionNode: FC<NodeProps<TransitionData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.TRANSITION}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.TRANSITION} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack data-testid="node-model">
            <NodeParams value={data.event || t('error.noSet.select')} />
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} id="target" />
      <CustomHandle type="source" id="source" position={Position.Right} isConnectable={1} />
    </>
  )
}
