import { FC } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Node } from 'reactflow'
import {
  Button,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  Text,
} from '@chakra-ui/react'
import { EditIcon } from '@chakra-ui/icons'
import { MdOutlineEventNote } from 'react-icons/md'

import { Adapter, Bridge } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'
import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'
import EventLogTable from '@/modules/EventLog/components/table/EventLogTable.tsx'
import Metrics from '@/modules/Metrics/Metrics.tsx'
import { ChartType } from '@/modules/Metrics/types.ts'

import { NodeTypes } from '../../types.ts'
import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'
import NodeNameCard from '../parts/NodeNameCard.tsx'

interface NodePropertyDrawerProps {
  nodeId: string
  selectedNode: Node<Bridge | Adapter>
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const NodePropertyDrawer: FC<NodePropertyDrawerProps> = ({ nodeId, isOpen, selectedNode, onClose, onEditEntity }) => {
  const { t } = useTranslation()

  return (
    <Drawer isOpen={isOpen} placement="right" size={'md'} onClose={onClose} variant={'hivemq'}>
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type }) as string}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
        </DrawerHeader>
        <DrawerBody display={'flex'} flexDirection={'column'} gap={6}>
          <NodeNameCard selectedNode={selectedNode} />
          <Metrics
            nodeId={nodeId}
            type={selectedNode.type as NodeTypes}
            adapterIDs={[selectedNode.data.id]}
            initMetrics={getDefaultMetricsFor(selectedNode)}
            defaultChartType={ChartType.SAMPLE}
          />
          <Card size={'sm'}>
            <CardHeader>
              <Text>
                {t('workspace.property.eventLog.header', { type: selectedNode.type, id: selectedNode.data.id })}
              </Text>
            </CardHeader>
            <CardBody>
              <EventLogTable globalSourceFilter={(selectedNode?.data as Adapter).id} variant={'summary'} />
            </CardBody>
            <CardFooter justifyContent={'flex-end'} pt={0}>
              <Button
                data-testid={'navigate-eventLog-filtered'}
                variant="link"
                as={RouterLink}
                // URL options not yet supported
                to={`/event-logs?source=${selectedNode.data.id}`}
                rightIcon={<MdOutlineEventNote />}
                size="sm"
              >
                {t('workspace.property.eventLog.showMore')}
              </Button>
            </CardFooter>
          </Card>
        </DrawerBody>
        <DrawerFooter borderTopWidth="1px">
          <Flex flexGrow={1} justifyContent={'flex-start'} gap={5}>
            <Button
              data-testid={'protocol-create-adapter'}
              variant={'outline'}
              size={'sm'}
              rightIcon={<EditIcon />}
              onClick={onEditEntity}
            >
              {t('workspace.property.modify', { context: selectedNode.type })}
            </Button>
            <ConnectionController
              type={selectedNode.type === NodeTypes.ADAPTER_NODE ? DeviceTypes.ADAPTER : DeviceTypes.BRIDGE}
              id={selectedNode.data.id}
              status={selectedNode.data.status}
            />
          </Flex>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default NodePropertyDrawer
