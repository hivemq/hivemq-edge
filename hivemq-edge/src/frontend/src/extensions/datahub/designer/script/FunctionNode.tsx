import { Code, HStack, Text, VStack } from '@chakra-ui/react'
import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'

import { DataHubNodeType, FunctionData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeIcon } from '@datahub/components/helpers'

export const FunctionNode: FC<NodeProps<FunctionData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.FUNCTION}/${id}`} {...props}>
        <VStack>
          <HStack>
            <NodeIcon type={DataHubNodeType.FUNCTION} />
            <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
            <VStack data-testid="node-model">
              <Code>{data?.name || t('error.noSet.select')}</Code>
            </VStack>
          </HStack>
        </VStack>
      </NodeWrapper>
      <CustomHandle type="source" position={Position.Bottom} id="source" />
    </>
  )
}
