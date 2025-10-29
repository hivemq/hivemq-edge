/**
 * Layout Controls Panel
 *
 * Panel with layout algorithm selector and apply button.
 * Appears in the workspace alongside other controls.
 */

import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, IconButton, Icon, Tooltip, useDisclosure } from '@chakra-ui/react'
import { LuSettings } from 'react-icons/lu'
import config from '@/config'
import Panel from '@/components/react-flow/Panel.tsx'
import { useKeyboardShortcut } from '@/hooks/useKeyboardShortcut'
import { useLayoutEngine } from '../../hooks/useLayoutEngine'
import useWorkspaceStore from '../../hooks/useWorkspaceStore'
import LayoutSelector from '../layout/LayoutSelector.tsx'
import ApplyLayoutButton from '../layout/ApplyLayoutButton.tsx'
import LayoutOptionsDrawer from '../layout/LayoutOptionsDrawer.tsx'
import LayoutPresetsManager from '../layout/LayoutPresetsManager.tsx'

const LayoutControlsPanel: FC = () => {
  const { t } = useTranslation()
  const { applyLayout } = useLayoutEngine()
  const { layoutConfig, setLayoutOptions } = useWorkspaceStore()
  const { isOpen, onOpen, onClose } = useDisclosure()

  // Keyboard shortcut: Ctrl/Cmd+L to apply layout
  useKeyboardShortcut({
    key: 'l',
    ctrl: true,
    callback: () => {
      applyLayout()
    },
    description: 'Apply current layout',
  })

  if (!config.features.WORKSPACE_AUTO_LAYOUT) {
    return null
  }

  return (
    <>
      <Panel
        position="top-left"
        data-testid="layout-controls-panel"
        aria-label={t('workspace.autoLayout.controls.aria-label')}
      >
        <HStack spacing={2} p={2} bg="white" _dark={{ bg: 'gray.800' }} borderRadius="md" boxShadow="md">
          <LayoutSelector />
          <ApplyLayoutButton />
          <LayoutPresetsManager />
          <Tooltip label="Layout Options" placement="bottom">
            <IconButton
              aria-label="Layout options"
              icon={<Icon as={LuSettings} />}
              size="sm"
              variant="ghost"
              onClick={onOpen}
            />
          </Tooltip>
        </HStack>
      </Panel>

      <LayoutOptionsDrawer
        isOpen={isOpen}
        onClose={onClose}
        algorithmType={layoutConfig.currentAlgorithm}
        options={layoutConfig.options as Record<string, unknown>}
        onOptionsChange={setLayoutOptions}
      />
    </>
  )
}

export default LayoutControlsPanel
