import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import Client from '@/components/MQTT/Client.tsx'

import { ClientFilterData } from '../../types.ts'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

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
          {data.clients?.map((t) => (
            <Client client={t} key={t} />
          ))}
        </VStack>
      </NodeWrapper>
      {data.clients?.map((t, index) => (
        <CustomHandle
          type="source"
          position={Position.Right}
          id={`${id}-${index}`}
          key={`${id}-${t}-${index}`}
          style={{
            top: `calc(var(--chakra-space-3) + 12px + ${index * 24}px + ${0.5 * index}rem)`,
          }}
        />
      ))}
    </>
  )
}
