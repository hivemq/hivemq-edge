import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody, HStack, Icon, StackDivider, Text } from '@chakra-ui/react'
import { PiBridgeThin, PiPlugsConnectedFill } from 'react-icons/pi'

import { Adapter, Bridge } from '@/api/__generated__'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'
import { useTranslation } from 'react-i18next'

interface NodeNameCardProps {
  selectedNode: Node<Bridge | Adapter>
}

const NodeNameCard: FC<NodeNameCardProps> = ({ selectedNode }) => {
  const { t } = useTranslation()
  const { type } = selectedNode

  const EntityIcon = useMemo(() => {
    if (type === NodeTypes.BRIDGE_NODE)
      return (
        <Icon
          data-testid={'node-type-icon'}
          data-nodeIcon={NodeTypes.BRIDGE_NODE}
          as={PiBridgeThin}
          fontSize={'20px'}
        />
      )
    return <Icon data-testid={'node-type-icon'} data-nodeIcon={NodeTypes.BRIDGE_NODE} as={PiPlugsConnectedFill} />
  }, [type])

  return (
    <Card size={'sm'} direction={'row'}>
      <CardBody>
        <HStack divider={<StackDivider />}>
          {EntityIcon}
          <Text>{t('workspace.device.type', { context: type })}</Text>
          {type === NodeTypes.ADAPTER_NODE && <Text>{(selectedNode as Node<Adapter>).data.type}</Text>}
          <Text noOfLines={1}>{selectedNode.data.id}</Text>
        </HStack>
      </CardBody>
    </Card>
  )
}

export default NodeNameCard
