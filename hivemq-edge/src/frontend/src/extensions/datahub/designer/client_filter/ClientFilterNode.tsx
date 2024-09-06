import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { ClientTag } from '@/components/MQTT/EntityTag.tsx'

import { ClientFilterData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'

export const ClientFilterNode: FC<NodeProps<ClientFilterData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <HStack>
          <VStack>
            <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
          </VStack>
        </HStack>
        <VStack ml={6} data-testid="node-model">
          {data.clients?.map((client) => (
            <ClientTag tagTitle={client} key={client} />
          ))}
        </VStack>
      </NodeWrapper>
      {data.clients?.map((client, index) => (
        <CustomHandle
          type="source"
          position={Position.Right}
          id={`${id}-${index}`}
          key={`${id}-${client}-${index}`}
          style={{
            top: `calc(var(--chakra-space-3) + 12px + ${index * 24}px + ${0.5 * index}rem)`,
          }}
        />
      ))}
    </>
  )
}
