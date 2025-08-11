import LicenseWarning from '@/modules/Pulse/components/activation/LicenseWarning.tsx'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
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

import type { NodeAssetsType, NodeTypes } from '@/modules/Workspace/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'

interface PulsePropertyDrawerProps {
  nodeId: string
  selectedNode: NodeAssetsType
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const PulsePropertyDrawer: FC<PulsePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()

  return (
    <Drawer isOpen={isOpen} placement="right" size="sm" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
          <NodeNameCard name={selectedNode.data.label} type={selectedNode.type as NodeTypes} />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <LicenseWarning />
        </DrawerBody>
        <DrawerFooter justifyContent="flex-end"></DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default PulsePropertyDrawer
