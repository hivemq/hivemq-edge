/**
 * Wizard Group Form (Step 1)
 *
 * Configuration form for creating a new group.
 * Reuses GroupMetadataEditor and GroupContentEditor but with proper wizard layout.
 */

import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useReactFlow } from '@xyflow/react'
import {
  Button,
  DrawerBody,
  DrawerCloseButton,
  DrawerFooter,
  DrawerHeader,
  Flex,
  Heading,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Text,
  VStack,
} from '@chakra-ui/react'

import GroupMetadataEditor from '../../parts/GroupMetadataEditor'
import GroupContentEditor from '../../parts/GroupContentEditor'
import type { Group, NodeGroupType } from '@/modules/Workspace/types'
import { NodeTypes } from '@/modules/Workspace/types'
import { useWizardState } from '@/modules/Workspace/hooks/useWizardStore'

interface WizardGroupFormProps {
  onSubmit: (groupData: { title: string; colorScheme: string }) => void
  onBack: () => void
}

/**
 * Step 1: Configure group
 * Follows standard wizard form pattern with DrawerHeader/Body/Footer
 */
const WizardGroupForm: FC<WizardGroupFormProps> = ({ onSubmit, onBack }) => {
  const { t } = useTranslation()
  const { selectedNodeIds } = useWizardState()
  const { getNodes } = useReactFlow()

  // Create a mock group node with the selected nodes as children
  const mockGroupNode = useMemo<NodeGroupType>(() => {
    const nodes = getNodes()
    const selectedNodes = nodes.filter((n) => selectedNodeIds.includes(n.id))

    return {
      id: 'wizard-group-preview',
      type: NodeTypes.CLUSTER_NODE,
      position: { x: 0, y: 0 },
      data: {
        title: t('workspace.grouping.untitled'),
        colorScheme: 'blue',
        childrenNodeIds: selectedNodes.map((n) => n.id),
        isOpen: true,
      },
    } as NodeGroupType
  }, [t, selectedNodeIds, getNodes])

  const handleSubmit = (groupData: Group) => {
    // Extract only the fields we need for wizard completion
    onSubmit({
      title: groupData.title,
      colorScheme: groupData.colorScheme || 'blue',
    })
  }

  return (
    <>
      <DrawerHeader borderBottomWidth="1px">
        <DrawerCloseButton onClick={onBack} />
        <Heading size="md">{t('workspace.wizard.group.configTitle')}</Heading>
      </DrawerHeader>

      <DrawerBody>
        <Tabs>
          <TabList>
            <Tab>{t('workspace.grouping.editor.tabs.config')}</Tab>
            <Tab>{t('workspace.grouping.editor.tabs.events')}</Tab>
            <Tab>{t('workspace.grouping.editor.tabs.metrics')}</Tab>
          </TabList>

          <TabPanels>
            <TabPanel px={0} as={VStack} alignItems="stretch">
              <GroupMetadataEditor group={mockGroupNode} onSubmit={handleSubmit} />
              <GroupContentEditor group={mockGroupNode} />
            </TabPanel>
            <TabPanel px={0} as={VStack} alignItems="stretch">
              <Text fontSize="sm" color="gray.500">
                {t('workspace.grouping.editor.eventLog.preview')}
              </Text>
            </TabPanel>
            <TabPanel px={0} as={VStack} alignItems="stretch">
              <Text fontSize="sm" color="gray.500">
                {t('workspace.grouping.editor.metrics.preview')}
              </Text>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </DrawerBody>

      <DrawerFooter borderTopWidth="1px">
        <Flex width="100%" justifyContent="space-between">
          <Button variant="outline" onClick={onBack} data-testid="wizard-group-form-back">
            {t('workspace.wizard.group.back')}
          </Button>
          <Button variant="primary" type="submit" form="group-form" data-testid="wizard-group-form-submit">
            {t('workspace.wizard.group.create')}
          </Button>
        </Flex>
      </DrawerFooter>
    </>
  )
}

export default WizardGroupForm
