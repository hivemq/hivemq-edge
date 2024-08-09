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
  HStack,
  Text,
} from '@chakra-ui/react'

import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import TopicExplorer from '@/modules/Workspace/components/topics/TopicExplorer.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

interface NodePropertyDrawerProps {
  nodeId: string
  selectedNode: Node
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const EdgePropertyDrawer: FC<NodePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()
  const { data, isLoading } = useGetEdgeTopics({ publishOnly: false })

  return (
    <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.property.header', { context: selectedNode.type })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text> {t('workspace.property.header', { context: selectedNode.type })}</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <HStack w="100%" h="600px" alignItems="flex-start">
            {isLoading ? <LoaderSpinner /> : <TopicExplorer data={data} />}
          </HStack>
        </DrawerBody>
        <DrawerFooter borderTopWidth="1px"></DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default EdgePropertyDrawer
