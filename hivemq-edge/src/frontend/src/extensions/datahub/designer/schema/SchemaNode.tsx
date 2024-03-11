import { HStack, Text, VStack } from '@chakra-ui/react'
import { FC, useMemo } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { RJSFSchema } from '@rjsf/utils'

import { DataHubNodeType, SchemaData, SchemaType } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeIcon, NodeParams } from '@datahub/components/helpers'

export const SchemaNode: FC<NodeProps<SchemaData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  const title = useMemo(() => {
    if (!data.schemaSource) return undefined

    if (data.type === SchemaType.PROTOBUF) return data.messageType

    try {
      const schema = JSON.parse(data.schemaSource) as RJSFSchema
      return schema.title
    } catch (error) {
      return undefined
    }
  }, [data.messageType, data.schemaSource, data.type])

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.SCHEMA}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.SCHEMA} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack data-testid="node-model">
            <NodeParams value={data?.type || t('error.noSet.select')} />
            <NodeParams value={title || t('error.noSet.select')} />
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="source" position={Position.Bottom} id="source" />
    </>
  )
}
