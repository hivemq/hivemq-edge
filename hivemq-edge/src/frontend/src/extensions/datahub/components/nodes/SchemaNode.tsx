import { HStack, Text, VStack } from '@chakra-ui/react'
import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'

import { DataHubNodeType, SchemaData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'
import { useTranslation } from 'react-i18next'

export const SchemaNode: FC<NodeProps<SchemaData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.SCHEMA}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.SCHEMA} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack>
            <Text data-testid="node-model">{data?.type || t('error.noSet.select')}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="source" position={Position.Bottom} id="source" />
    </>
  )
}
