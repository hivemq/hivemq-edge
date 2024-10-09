import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { getIncomers, Node } from 'reactflow'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Text,
} from '@chakra-ui/react'

import { Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import DeviceMetadataViewer from '@/modules/Device/components/DeviceMetadataViewer.tsx'
import DeviceTagList from '@/modules/Device/components/DeviceTagList.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { DeviceMetadata } from '@/modules/Workspace/types.ts'

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
  const [adapterNode] = getIncomers<Adapter, any>(selectedNode, nodes, edges)

  const protocol = data?.items?.find((e) => e.id === adapterNode.data.type)

  if (isLoading) return <LoaderSpinner />
  if (isError) return <ErrorMessage message={t('device.errors.noAdapter')} />

  return (
    <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <DrawerBody display="flex" flexDirection="column" gap={6}>
            <DeviceMetadataViewer protocolAdapter={protocol} />
            <DeviceTagList adapter={adapterNode.data} />
          </DrawerBody>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default DevicePropertyDrawer
