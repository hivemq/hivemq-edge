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
  VStack,
} from '@chakra-ui/react'
import { EditIcon } from '@chakra-ui/icons'
import { MdOutlineEventNote } from 'react-icons/md'

import { Adapter, Bridge } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'
import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'
import Metrics from '@/modules/Welcome/components/Metrics.tsx'
import EventLogTable from '@/modules/EventLog/components/table/EventLogTable.tsx'

import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'

interface NodePropertyDrawerProps {
  selectedNode: Node<Bridge | Adapter>
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}
const NodePropertyDrawer: FC<NodePropertyDrawerProps> = ({ isOpen, selectedNode, onClose, onEditEntity }) => {
  const { t } = useTranslation()

  return (
    <Drawer isOpen={isOpen} placement="right" size={'md'} onClose={onClose}>
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text>{t('workspace.observability.adapter.header')}</Text>
        </DrawerHeader>
        <DrawerBody>
          <VStack gap={4} alignItems={'stretch'}>
            <Metrics initMetrics={getDefaultMetricsFor(selectedNode)} />
            <Card size={'sm'}>
              <CardHeader>
                <Text>
                  {t('workspace.observability.eventLog.header', { type: selectedNode.type, id: selectedNode.data.id })}
                </Text>
              </CardHeader>
              <CardBody>
                <EventLogTable globalSourceFilter={(selectedNode?.data as Adapter).id} variant={'summary'} />
              </CardBody>
              <CardFooter justifyContent={'flex-end'} pt={0}>
                <Button
                  variant="link"
                  as={RouterLink}
                  // URL options not yet supported
                  to={`/event-logs?source=${selectedNode.data.id}`}
                  rightIcon={<MdOutlineEventNote />}
                  size="sm"
                >
                  {t('workspace.observability.eventLog.showMore')}
                </Button>
              </CardFooter>
            </Card>
          </VStack>
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
              {t('workspace.observability.adapter.modify')}
            </Button>
            <ConnectionController
              type={DeviceTypes.ADAPTER}
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
