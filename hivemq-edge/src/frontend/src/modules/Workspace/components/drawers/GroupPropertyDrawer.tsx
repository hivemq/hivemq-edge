import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Node } from 'reactflow'
import { Link as RouterLink } from 'react-router-dom'
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
  DrawerHeader,
  DrawerOverlay,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Text,
  VStack,
} from '@chakra-ui/react'
import { MdOutlineEventNote } from 'react-icons/md'

import MetricsContainer from '@/modules/Metrics/MetricsContainer.tsx'

import { Group, NodeTypes } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'
import GroupMetadataEditor from '../parts/GroupMetadataEditor.tsx'
import { ChartType, MetricsFilter } from '@/modules/Metrics/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import GroupContentEditor from '@/modules/Workspace/components/parts/GroupContentEditor.tsx'
import EventLogTable from '@/modules/EventLog/components/table/EventLogTable.tsx'

interface GroupPropertyDrawerProps {
  nodeId: string
  selectedNode: Node<Group>
  nodes: Node[]
  isOpen: boolean
  showConfig?: boolean
  onClose: () => void
  onEditEntity: () => void
}

const GroupPropertyDrawer: FC<GroupPropertyDrawerProps> = ({
  nodeId,
  isOpen,
  showConfig = false,
  selectedNode,
  nodes,
  onClose,
}) => {
  const { t } = useTranslation()
  const { onGroupSetData } = useWorkspaceStore()

  const adapterIDs = selectedNode.data.childrenNodeIds.map<Node | undefined>((e) => nodes.find((x) => x.id === e))
  const metrics = adapterIDs.map((x) => (x ? getDefaultMetricsFor(x) : [])).flat()

  const linkEventLog = useMemo(() => {
    const searchParams = new URLSearchParams()
    for (const node of adapterIDs) {
      if (node) searchParams.append('source', node.data.id)
    }
    return `/event-logs?${searchParams.toString()}`
  }, [adapterIDs])

  const panelTitle = showConfig
    ? t('workspace.property.header', { context: selectedNode.type })
    : t('workspace.observability.header', { context: selectedNode.type })

  const renderMetricsContainer = () => (
    <MetricsContainer
      nodeId={nodeId}
      type={selectedNode.type as NodeTypes}
      filters={adapterIDs.reduce<MetricsFilter[]>((acc, cur) => {
        if (cur && cur.type === NodeTypes.ADAPTER_NODE) {
          acc.push({ id: cur.data.id, type: `com.hivemq.edge.protocol-adapters.${cur.data.type}` })
        }
        return acc
      }, [])}
      initMetrics={metrics}
      defaultChartType={showConfig ? ChartType.SAMPLE : undefined}
    />
  )

  const renderGroupTabs = () => (
    <Tabs>
      <TabList>
        <Tab>{t('workspace.grouping.editor.tabs.config')}</Tab>
        <Tab>{t('workspace.grouping.editor.tabs.events')}</Tab>
        <Tab>{t('workspace.grouping.editor.tabs.metrics')}</Tab>
      </TabList>

      <TabPanels>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <GroupMetadataEditor
            group={selectedNode}
            onSubmit={(group) => {
              onGroupSetData(nodeId, group)
            }}
          />
          <GroupContentEditor group={selectedNode} />
        </TabPanel>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <Card size="sm">
            <CardHeader>
              <Text>{t('workspace.grouping.editor.eventLog.header')}</Text>
            </CardHeader>
            <CardBody>
              <EventLogTable
                globalSourceFilter={adapterIDs.map((e) => e?.data.id)}
                variant="summary"
                maxEvents={10}
                isSingleSource={false}
              />
            </CardBody>
            <CardFooter justifyContent="flex-end" pt={0}>
              <Button
                data-testid="navigate-eventLog-filtered"
                variant="link"
                as={RouterLink}
                // URL options not yet supported
                to={linkEventLog}
                rightIcon={<MdOutlineEventNote />}
                size="sm"
              >
                {t('workspace.grouping.editor.eventLog.showMore')}
              </Button>
            </CardFooter>
          </Card>
        </TabPanel>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          {renderMetricsContainer()}
        </TabPanel>
      </TabPanels>
    </Tabs>
  )

  return (
    <Drawer isOpen={isOpen} placement="right" size="lg" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={panelTitle}>
        <DrawerCloseButton />

        <DrawerHeader>
          <Text data-testid="group-panel-title">{panelTitle}</Text>
          <NodeNameCard
            description={t('workspace.device.type', { context: selectedNode.type })}
            type={selectedNode.type as NodeTypes}
            name={selectedNode.data.title}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {showConfig && renderGroupTabs()}
          {!showConfig && renderMetricsContainer()}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default GroupPropertyDrawer
