import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody, HStack, Icon, StackDivider, Tag, Text, VStack } from '@chakra-ui/react'
import { PiBridgeThin, PiPlugsConnectedFill } from 'react-icons/pi'
import { useTranslation } from 'react-i18next'

import { Adapter, Bridge } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'

import { NodeTypes } from '../../types.ts'

interface NodeNameCardProps {
  selectedNode: Node<Bridge | Adapter>
}

const NodeNameCard: FC<NodeNameCardProps> = ({ selectedNode }) => {
  const { t } = useTranslation()
  const { data } = useGetAdapterTypes()
  const { type } = selectedNode

  const adapterType = useMemo(() => {
    if (!data) return undefined
    if (type === NodeTypes.BRIDGE_NODE) return undefined

    const adapterType = (selectedNode as Node<Adapter>).data.type
    return data?.items?.find((e) => e.id === adapterType)
  }, [data, selectedNode, type])

  const EntityIcon = useMemo(() => {
    if (type === NodeTypes.BRIDGE_NODE)
      return (
        <Icon
          data-testid={'node-type-icon'}
          data-nodeicon={NodeTypes.BRIDGE_NODE}
          as={PiBridgeThin}
          fontSize={'40px'}
        />
      )
    return <Icon data-testid={'node-type-icon'} data-nodeicon={NodeTypes.ADAPTER_NODE} as={PiPlugsConnectedFill} />
  }, [type])

  return (
    <Card size={'sm'} direction={'row'}>
      <CardBody>
        <HStack divider={<StackDivider />}>
          <VStack>
            {EntityIcon}
            <Tag data-testid={'node-type-text'} textTransform={'uppercase'}>
              {t('workspace.device.type', { context: type })}{' '}
            </Tag>
          </VStack>

          <VStack alignItems={'flex-start'}>
            {adapterType && <Text data-testid={'node-adapter-type'}>{adapterType.name}</Text>}
            <Text data-testid={'node-name'} noOfLines={1}>
              {selectedNode.data.id}
            </Text>
          </VStack>
        </HStack>
      </CardBody>
    </Card>
  )
}

export default NodeNameCard
