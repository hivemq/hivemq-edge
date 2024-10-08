import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Node } from 'reactflow'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Text,
} from '@chakra-ui/react'

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

  return (
    <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}></DrawerBody>
        <DrawerFooter borderTopWidth="1px"></DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default DevicePropertyDrawer
