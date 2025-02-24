import type { FC } from 'react'
import { useMemo } from 'react'
import { Card, CardBody, HStack, Icon, Image, StackDivider, Text, VStack } from '@chakra-ui/react'
import { PiBridgeThin } from 'react-icons/pi'
import { GrStatusUnknown } from 'react-icons/gr'
import { ImMakeGroup } from 'react-icons/im'
import { MdScheduleSend } from 'react-icons/md'

import edgeLogo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'

import { deviceCategoryIcon, ProtocolAdapterCategoryName } from '@/modules/Workspace/utils/adapter.utils.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface NodeNameCardProps {
  type: NodeTypes
  name?: string
  description?: string
  icon?: string
}

const NodeNameCard: FC<NodeNameCardProps> = ({ name, type, description, icon }) => {
  const EntityIcon = useMemo(() => {
    switch (type) {
      case NodeTypes.ADAPTER_NODE:
        return (
          <Image
            aria-label={type}
            boxSize="20px"
            objectFit="scale-down"
            src={icon}
            data-testid="node-type-icon"
            data-nodeicon={type}
          />
        )

      case NodeTypes.BRIDGE_NODE:
        return <Icon data-testid="node-type-icon" data-nodeicon={type} as={PiBridgeThin} fontSize="24px" />
      case NodeTypes.CLUSTER_NODE:
        return <Icon data-testid="node-type-icon" data-nodeicon={type} as={ImMakeGroup} fontSize="24px" />
      case NodeTypes.EDGE_NODE:
        return (
          <Image data-testid="node-type-icon" data-nodeicon={type} objectFit="cover" w="24px" src={edgeLogo} alt="SS" />
        )
      case NodeTypes.COMBINER_NODE:
        return <Icon data-testid="node-type-icon" data-nodeicon={type} as={MdScheduleSend} fontSize="24px" />

      case NodeTypes.DEVICE_NODE:
        return (
          <Icon
            data-testid="node-type-icon"
            data-nodeicon={type}
            as={deviceCategoryIcon[ProtocolAdapterCategoryName.SIMULATION]}
            fontSize="24px"
          />
        )
      default:
        return <Icon data-testid="node-type-icon" data-nodeicon={type} as={GrStatusUnknown} />
    }
  }, [icon, type])

  return (
    <Card size="sm" direction="row" fontSize="sm">
      <CardBody>
        <HStack divider={<StackDivider />}>
          {EntityIcon}
          <VStack alignItems="flex-start" gap={0}>
            {name && (
              <Text data-testid="node-name" noOfLines={1}>
                {name}
              </Text>
            )}
            {description && (
              <Text data-testid="node-description" noOfLines={1} fontWeight="normal">
                {description}
              </Text>
            )}
          </VStack>
        </HStack>
      </CardBody>
    </Card>
  )
}

export default NodeNameCard
