import { FC, useState } from 'react'
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

import TopicExplorer from '@/modules/Workspace/components/topics/TopicExplorer.tsx'
import MetadataExplorer from '@/modules/Workspace/components/topics/MetadataExplorer.tsx'
import TopicSampler from '@/modules/Workspace/components/topics/TopicSampler.tsx'

interface NodePropertyDrawerProps {
  nodeId: string
  selectedNode: Node
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const EdgePropertyDrawer: FC<NodePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()
  const [selectedTopic, setSelectedTopic] = useState<string | undefined>()
  const isWildcard = Boolean(selectedTopic && selectedTopic.includes('#'))

  return (
    <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <TopicExplorer onSelect={(topic) => setSelectedTopic(topic)} />
          {selectedTopic && !isWildcard && <MetadataExplorer topic={selectedTopic} />}
          {selectedTopic && isWildcard && <TopicSampler topic={selectedTopic} />}
        </DrawerBody>
        <DrawerFooter borderTopWidth="1px"></DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default EdgePropertyDrawer