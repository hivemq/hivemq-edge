import { HStack, Text, VStack } from '@chakra-ui/react'
import { FC, useMemo } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'

import { DataHubNodeType, SchemaData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeIcon, NodeParams } from '@datahub/components/helpers'
import { renderResourceName } from '@datahub/utils/node.utils.ts'

export const SchemaNode: FC<NodeProps<SchemaData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  const title = useMemo(() => renderResourceName(data.name, data.version, t), [data.name, data.version, t])

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.SCHEMA}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.SCHEMA} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack data-testid="node-model">
            <NodeParams value={data?.type || t('error.noSet.select')} />
            <NodeParams value={title} />
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="source" position={Position.Bottom} id="source" />
    </>
  )
}
