import type { FC } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { Node } from '@xyflow/react'
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

import type { Adapter, Bridge } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'
import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'
import EventLogTable from '@/modules/EventLog/components/table/EventLogTable.tsx'
import MetricsContainer from '@/modules/Metrics/MetricsContainer.tsx'
import { ChartType } from '@/modules/Metrics/types.ts'

import { NodeTypes } from '../../types.ts'
import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'

interface NodePropertyDrawerProps {
  nodeId: string
  selectedNode: Node<Bridge | Adapter>
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const NodePropertyDrawer: FC<NodePropertyDrawerProps> = ({ nodeId, isOpen, selectedNode, onClose, onEditEntity }) => {
  const { t } = useTranslation()
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol =
    selectedNode.type === NodeTypes.ADAPTER_NODE
      ? protocols?.items?.find((e) => e.id === (selectedNode as Node<Adapter>).data.type)
      : undefined

  return (
    <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text>{t('workspace.property.header', { context: selectedNode.type })}</Text>
          <NodeNameCard
            name={selectedNode.data.id}
            type={selectedNode.type as NodeTypes}
            icon={adapterProtocol?.logoUrl}
            description={adapterProtocol?.name}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <MetricsContainer
            nodeId={nodeId}
            type={selectedNode.type as NodeTypes}
            filters={[
              {
                id: selectedNode.data.id,
                type:
                  selectedNode.type === NodeTypes.ADAPTER_NODE
                    ? `com.hivemq.edge.protocol-adapters.${(selectedNode as Node<Adapter>).data.type}`
                    : 'com.hivemq.edge.bridge',
              },
            ]}
            initMetrics={getDefaultMetricsFor(selectedNode)}
            defaultChartType={ChartType.SAMPLE}
          />
          <Card size="sm">
            <CardHeader>
              <Text>
                {t('workspace.property.eventLog.header', { type: selectedNode.type, id: selectedNode.data.id })}
              </Text>
            </CardHeader>
            <CardBody>
              <EventLogTable
                globalSourceFilter={[(selectedNode?.data as Adapter).id]}
                variant="summary"
                isSingleSource
              />
            </CardBody>
            <CardFooter justifyContent="flex-end" pt={0}>
              <Button
                data-testid="navigate-eventLog-filtered"
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
          <Flex flexGrow={1} justifyContent="flex-start" gap={5}>
            <Button
              data-testid="protocol-create-adapter"
              variant="outline"
              size="sm"
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
