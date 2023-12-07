import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Node } from 'reactflow'
import { Box, Drawer, DrawerBody, DrawerCloseButton, DrawerContent, DrawerHeader, Text } from '@chakra-ui/react'

import Metrics from '@/modules/Metrics/Metrics.tsx'

import { getDefaultMetricsFor } from '../../utils/nodes-utils.ts'
import { Group, NodeTypes } from '../../types.ts'

interface LinkPropertyDrawerProps {
  nodeId: string
  selectedNode: Node<Group>
  nodes: Node[]
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const GroupPropertyDrawer: FC<LinkPropertyDrawerProps> = ({ nodeId, isOpen, selectedNode, nodes, onClose }) => {
  const { t } = useTranslation()

  const adapterIDs = selectedNode.data.childrenNodeIds.map<Node | undefined>((e) => nodes.find((x) => x.id === e))
  const metrics = adapterIDs.map((x) => (x ? getDefaultMetricsFor(x) : [])).flat()

  return (
    <Drawer isOpen={isOpen} placement="right" size={'lg'} onClose={onClose}>
      {/*<DrawerOverlay />*/}
      <DrawerContent aria-label={t('workspace.observability.header', { context: selectedNode.type }) as string}>
        <DrawerCloseButton />

        <DrawerHeader>
          <Text data-testid={'group-panel-title'}>
            {t('workspace.observability.header', { context: selectedNode.type })}
          </Text>
          <Box data-testid={'group-panel-keys'}>
            {selectedNode.data.childrenNodeIds.map((e) => (
              <Text key={e}>
                {t('workspace.device.type', { context: NodeTypes.ADAPTER_NODE })}:{' '}
                {nodes.find((x) => x.id === e)?.data.id}
              </Text>
            ))}
          </Box>
        </DrawerHeader>
        <DrawerBody>
          <Metrics
            nodeId={nodeId}
            type={selectedNode.type as NodeTypes}
            adapterIDs={adapterIDs.map((e) => e?.data.id)}
            initMetrics={metrics}
          />
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default GroupPropertyDrawer
