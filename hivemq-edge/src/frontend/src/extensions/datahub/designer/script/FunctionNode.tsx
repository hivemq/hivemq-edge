import { HStack, VStack } from '@chakra-ui/react'
import { FC, useMemo } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'

import { DataHubNodeType, FunctionData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { NodeParams } from '@datahub/components/helpers'
import { renderResourceName } from '@datahub/utils/node.utils.ts'

export const FunctionNode: FC<NodeProps<FunctionData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data } = props

  const title = useMemo(() => renderResourceName(data.name, data.version, t), [data.name, data.version, t])

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.FUNCTION}/${id}`} {...props}>
        <VStack>
          <HStack>
            <VStack data-testid="node-model">
              <NodeParams value={title || t('error.noSet.select')} />
            </VStack>
          </HStack>
        </VStack>
      </NodeWrapper>
      <CustomHandle
        type="source"
        position={Position.Right}
        id="source"
        style={{
          top: `calc(var(--chakra-space-3) + 12px + 48px `,
        }}
      />
    </>
  )
}
