import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { Node } from '@xyflow/react'
import { Drawer, DrawerBody, DrawerCloseButton, DrawerContent, DrawerHeader, Text } from '@chakra-ui/react'

import type { Adapter, Bridge, Combiner } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import MetricsContainer from '@/modules/Metrics/MetricsContainer.tsx'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { getDefaultMetricsFor } from '@/modules/Workspace/utils/nodes-utils.ts'

interface LinkPropertyDrawerProps {
  nodeId: string
  selectedNode: Node<Bridge | Adapter | Combiner>
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const LinkPropertyDrawer: FC<LinkPropertyDrawerProps> = ({ nodeId, isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol =
    selectedNode.type === NodeTypes.ADAPTER_NODE
      ? protocols?.items?.find((e) => e.id === (selectedNode as Node<Adapter>).data.type)
      : undefined

  const displayInfo = useMemo(() => {
    if (selectedNode.type === NodeTypes.COMBINER_NODE)
      return { name: (selectedNode.data as Combiner).name, description: t('combiner.type') }
    return { name: selectedNode.data.id, description: adapterProtocol?.name }
  }, [adapterProtocol?.name, selectedNode.data, selectedNode.type, t])

  return (
    <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} variant="hivemq">
      <DrawerContent>
        <DrawerCloseButton />

        <DrawerHeader>
          <Text>{t('workspace.observability.header', { context: selectedNode.type })}</Text>
          <NodeNameCard
            name={displayInfo.name}
            type={selectedNode.type as NodeTypes}
            icon={adapterProtocol?.logoUrl}
            description={displayInfo.description}
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
          />
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default LinkPropertyDrawer
