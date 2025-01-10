import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Node } from 'reactflow'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Text,
} from '@chakra-ui/react'

import MetricsContainer from '@/modules/Metrics/MetricsContainer.tsx'

import { Group, NodeTypes } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'
import GroupMetadataEditor from '../parts/GroupMetadataEditor.tsx'
import { ChartType, MetricsFilter } from '@/modules/Metrics/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'

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

  const panelTitle = showConfig
    ? t('workspace.property.header', { context: selectedNode.type })
    : t('workspace.observability.header', { context: selectedNode.type })

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
          {showConfig && (
            <GroupMetadataEditor
              group={selectedNode}
              onSubmit={(group) => {
                onGroupSetData(nodeId, group)
              }}
            />
          )}
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
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default GroupPropertyDrawer
