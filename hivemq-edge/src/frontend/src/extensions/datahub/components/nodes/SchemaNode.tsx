import { HStack, Text, VStack } from '@chakra-ui/react'
import { FC, useMemo } from 'react'
import { NodeProps, Position } from 'reactflow'

import { DataHubNodeType, SchemaData, SchemaType } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import NodeParams from '../helpers/NodeParams.tsx'

import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'
import { useTranslation } from 'react-i18next'
import { RJSFSchema } from '@rjsf/utils'

export const SchemaNode: FC<NodeProps<SchemaData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  const title = useMemo(() => {
    if (!data.schemaSource) return undefined

    // TODO[NVL] PROTOBUF parser needed for extracting title out of the source
    if (data.type !== SchemaType.JSON) return undefined

    try {
      const schema = JSON.parse(data.schemaSource) as RJSFSchema
      return schema.title
    } catch (e) {
      return undefined
    }
  }, [data.schemaSource, data.type])

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
