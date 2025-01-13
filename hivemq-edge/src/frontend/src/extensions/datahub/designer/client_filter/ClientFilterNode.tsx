import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { VStack } from '@chakra-ui/react'

import { ClientTag } from '@/components/MQTT/EntityTag.tsx'

import { ClientFilterData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'

export const ClientFilterNode: FC<NodeProps<ClientFilterData>> = (props) => {
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <VStack ml={6} data-testid="node-model" alignItems="flex-end">
          {data.clients?.map((client) => (
            <ClientTag tagTitle={client} key={client} data-testid="client-wrapper" />
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
            top: `calc(var(--chakra-space-3) + 12px + 48px + ${index * 24}px + ${0.5 * index}rem)`,
          }}
        />
      ))}
    </>
  )
}
