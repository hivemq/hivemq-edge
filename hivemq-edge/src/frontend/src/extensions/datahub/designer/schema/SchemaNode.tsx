import { HStack, VStack } from '@chakra-ui/react'
import type { FC } from 'react'
import { useMemo } from 'react'
import type { NodeProps } from 'reactflow'
import { Position } from 'reactflow'
import { useTranslation } from 'react-i18next'

import type { SchemaData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeParams } from '@datahub/components/helpers'
import { renderResourceName } from '@datahub/utils/node.utils.ts'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

export const SchemaNode: FC<NodeProps<SchemaData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data } = props

  const title = useMemo(() => renderResourceName(data.name, data.version, t), [data.name, data.version, t])

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.SCHEMA}/${id}`} {...props}>
        <HStack justifyContent="flex-end">
          <VStack data-testid="node-model" alignItems="flex-end">
            <NodeParams value={data?.type || t('error.noSet.select')} />
            <NodeParams value={title} />
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle
        type="source"
        position={Position.Right}
        id="source"
        style={{
          top: getHandlePosition(0),
        }}
      />
    </>
  )
}
