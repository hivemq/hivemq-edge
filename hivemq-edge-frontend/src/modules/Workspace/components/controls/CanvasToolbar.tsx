import type { FC } from 'react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Box,
  VStack,
  Icon,
  Divider,
  Tooltip,
  IconButton as ChakraIconButton,
  useDisclosure,
  useBreakpointValue,
} from '@chakra-ui/react'
import { ChevronRightIcon, SearchIcon } from '@chakra-ui/icons'
import { LuNetwork, LuSettings } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import Panel from '@/components/react-flow/Panel.tsx'
import SearchEntities from '@/modules/Workspace/components/filters/SearchEntities.tsx'
import DrawerFilterToolbox from '@/modules/Workspace/components/filters/DrawerFilterToolbox.tsx'
import LayoutSelector from '@/modules/Workspace/components/layout/LayoutSelector.tsx'
import ApplyLayoutButton from '@/modules/Workspace/components/layout/ApplyLayoutButton.tsx'
import LayoutPresetsManager from '@/modules/Workspace/components/layout/LayoutPresetsManager.tsx'
import LayoutOptionsDrawer from '@/modules/Workspace/components/layout/LayoutOptionsDrawer.tsx'
import CreateEntityButton from '@/modules/Workspace/components/wizard/CreateEntityButton.tsx'
import { useLayoutEngine } from '@/modules/Workspace/hooks/useLayoutEngine'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { useKeyboardShortcut } from '@/hooks/useKeyboardShortcut'
import { ANIMATION } from '@/modules/Theme/utils.ts'

const CanvasToolbar: FC = () => {
  const { t } = useTranslation()
  const [expanded, setExpanded] = useState(false)
  const [contentVisible, setContentVisible] = useState(false)

  const dividerOrientation = useBreakpointValue<'horizontal' | 'vertical'>({
    base: 'horizontal',
    xl: 'vertical',
  })
  const tooltipPlacement = useBreakpointValue<'top' | 'bottom'>({
    base: 'top',
    xl: 'bottom',
  })

  const { applyLayout } = useLayoutEngine()
  const { layoutConfig } = useWorkspaceStore()
  const { isOpen: isLayoutDrawerOpen, onOpen: onLayoutDrawerOpen, onClose: onLayoutDrawerClose } = useDisclosure()

  useKeyboardShortcut({
    key: 'l',
    ctrl: true,
    callback: () => {
      applyLayout()
    },
    description: 'Apply current layout',
  })

  useEffect(() => {
    let timeout: NodeJS.Timeout
    if (expanded) {
      setContentVisible(true)
    } else {
      timeout = setTimeout(() => setContentVisible(false), ANIMATION.TOOLBAR_ANIMATION_DURATION_MS)
    }
    return () => clearTimeout(timeout)
  }, [expanded])

  return (
    <>
      <Panel
        position="top-left"
        data-testid="content-toolbar"
        role="group"
        aria-label={t('workspace.canvas.toolbar.search-filter')}
      >
        <Box
          m="1.2px"
          display="flex"
          flexDirection={{ base: 'column', xl: 'row' }}
          transition="all 0.4s cubic-bezier(0.4,0,0.2,1)"
          boxShadow="md"
          borderRadius="md"
          position="relative"
          overflow="hidden"
          bg="white"
          _dark={{ bg: 'gray.800' }}
          _focusWithin={{ boxShadow: 'outline' }}
        >
          {/* Wizard Trigger Button - Always visible */}
          <Box m={2}>
            <CreateEntityButton />
          </Box>
          <Divider orientation={dividerOrientation} borderColor="gray.300" _dark={{ borderColor: 'gray.600' }} />

          <IconButton
            data-testid="toolbox-search-expand"
            aria-label={t('workspace.controls.expand')}
            aria-expanded="false"
            aria-controls="workspace-toolbar-content"
            icon={
              <>
                <SearchIcon mr="2px" />
                <Icon as={LuNetwork} boxSize="18px" />
                <Icon as={ChevronRightIcon} boxSize="24px" transform={{ base: 'rotate(90deg)', xl: 'rotate(0deg)' }} />
              </>
            }
            onClick={() => setExpanded(true)}
            variant="ghost"
            size="sm"
            m={2}
            // minWidth="120px"
            display={expanded ? 'none' : 'inline-flex'}
          />

          <Box
            id="workspace-toolbar-content"
            role="region"
            aria-label={t('workspace.canvas.toolbar.search-filter')}
            display={contentVisible ? 'flex' : 'none'}
            flexDirection={{ base: 'column', xl: 'row' }}
            gap={{ base: 3, xl: 2 }}
            p={{ base: 3, xl: 2 }}
            width="100%"
            opacity={expanded ? 1 : 0}
            transition="opacity 0.4s cubic-bezier(0.4,0,0.2,1)"
            pointerEvents={contentVisible ? 'auto' : 'none'}
          >
            <VStack
              spacing={{ base: 2, xl: 0 }}
              align="stretch"
              flex={{ base: '1', xl: 'initial' }}
              sx={{
                '& > *': {
                  width: { base: '100%', xl: 'auto' },
                },
              }}
            >
              <Box display="flex" flexDirection={{ base: 'column', md: 'row' }} gap={2}>
                <SearchEntities />
                <DrawerFilterToolbox />
              </Box>
            </VStack>
            <Divider orientation={dividerOrientation} borderColor="gray.300" _dark={{ borderColor: 'gray.600' }} />

            <VStack
              role="region"
              data-testid="layout-controls-panel"
              aria-label={t('workspace.autoLayout.controls.aria-label')}
              spacing={{ base: 2, xl: 0 }}
              align="stretch"
              flex={{ base: '1', xl: 'initial' }}
            >
              <Box
                display="flex"
                flexDirection={{ base: 'column', md: 'row', xl: 'row' }}
                gap={2}
                sx={{
                  '& > *': {
                    width: { base: '100%', md: 'auto' },
                  },
                }}
              >
                <LayoutSelector />
                <ApplyLayoutButton />
                <Box display="flex" gap={2} width="fit-content">
                  <LayoutPresetsManager />
                  <Tooltip label={t('workspace.autoLayout.options.title')} placement={tooltipPlacement}>
                    <ChakraIconButton
                      data-testid="workspace-layout-options"
                      aria-label={t('workspace.autoLayout.options.title')}
                      icon={<Icon as={LuSettings} />}
                      size="sm"
                      variant="ghost"
                      onClick={onLayoutDrawerOpen}
                      width={{ base: '100%', md: 'auto' }}
                    />
                  </Tooltip>
                </Box>
              </Box>
            </VStack>
            <Divider orientation={dividerOrientation} borderColor="gray.300" _dark={{ borderColor: 'gray.600' }} />
            <IconButton
              data-testid="toolbox-search-collapse"
              aria-label={t('workspace.controls.collapse')}
              aria-expanded="true"
              aria-controls="workspace-toolbar-content"
              icon={
                <Icon as={ChevronRightIcon} boxSize="24px" transform={{ base: 'rotate(-90deg)', xl: 'rotate(0deg)' }} />
              }
              onClick={() => setExpanded(false)}
              variant="ghost"
              size="sm"
              alignSelf={{ base: 'center', xl: 'center' }}
              mt={{ base: 2, xl: 0 }}
              mb={{ base: 0, xl: 0 }}
            />
          </Box>
        </Box>
      </Panel>

      {/* Layout Options Drawer */}

      <LayoutOptionsDrawer
        isOpen={isLayoutDrawerOpen}
        onClose={onLayoutDrawerClose}
        algorithmType={layoutConfig.currentAlgorithm}
        options={layoutConfig.options}
      />
    </>
  )
}

export default CanvasToolbar
