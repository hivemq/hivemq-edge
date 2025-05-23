import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { Node } from '@xyflow/react'
import { getIncomers } from '@xyflow/react'
import {
  Button,
  ButtonGroup,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Text,
} from '@chakra-ui/react'

import type { Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import DeviceTagList from '@/modules/Device/components/DeviceTagList.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import type { DeviceMetadata, NodeTypes } from '@/modules/Workspace/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'

interface DevicePropertyDrawerProps {
  nodeId: string
  selectedNode: Node<DeviceMetadata>
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const DevicePropertyDrawer: FC<DevicePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()
  const { nodes, edges } = useWorkspaceStore()
  const { data, isError, isLoading } = useGetAdapterTypes()

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const incomers = getIncomers<Adapter, any>(selectedNode, nodes, edges)
  const adapter = incomers.find(Boolean)?.data
  const protocol = data?.items?.find((e) => e.id === adapter?.type)

  if (isLoading) return <LoaderSpinner />
  if (isError || !adapter || !protocol) return <ErrorMessage message={t('device.errors.noAdapter')} />

  return (
    <Drawer isOpen={isOpen} placement="right" size="lg" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
          <NodeNameCard
            name={selectedNode.data.name}
            type={selectedNode.type as NodeTypes}
            description={selectedNode.data.id}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <DeviceTagList adapter={adapter} onClose={onClose} />
        </DrawerBody>
        <DrawerFooter justifyContent="flex-end">
          <ButtonGroup>
            {selectedNode && (
              <Button variant="primary" type="submit" form="tag-listing-form">
                {t('device.drawer.table.actions.submit')}
              </Button>
            )}
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default DevicePropertyDrawer
