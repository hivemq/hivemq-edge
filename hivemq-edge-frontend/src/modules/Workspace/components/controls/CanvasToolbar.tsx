import type { FC } from 'react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, HStack, Icon } from '@chakra-ui/react'
import { ChevronLeftIcon, ChevronRightIcon, SearchIcon } from '@chakra-ui/icons'

import IconButton from '@/components/Chakra/IconButton.tsx'
import Panel from '@/components/react-flow/Panel.tsx'
import SearchEntities from '@/modules/Workspace/components/filters/SearchEntities.tsx'
import DrawerFilterToolbox from '@/modules/Workspace/components/filters/DrawerFilterToolbox.tsx'
import { ANIMATION } from '@/modules/Theme/utils.ts'

const CanvasToolbar: FC = () => {
  const { t } = useTranslation()
  const [expanded, setExpanded] = useState(false)
  const [contentVisible, setContentVisible] = useState(false)

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
    <Panel
      position="top-right"
      data-testid="content-toolbar"
      role="group"
      aria-label={t('workspace.canvas.toolbar.search-filter')}
    >
      <Box
        m="1.2px"
        display="flex"
        alignItems="center"
        transition="max-width 0.4s cubic-bezier(0.4,0,0.2,1)"
        maxWidth={expanded ? '1280px' : '80px'}
        width="auto"
        minHeight="40px"
        boxShadow="md"
        borderRadius="md"
        position="relative"
        overflow="hidden"
        _focusWithin={{ boxShadow: 'outline' }}
      >
        <IconButton
          data-testid="toolbox-search-expand"
          aria-label={t('workspace.controls.expand')}
          icon={
            <>
              <Icon as={ChevronLeftIcon} boxSize="24px" />
              <SearchIcon />
            </>
          }
          onClick={() => setExpanded(true)}
          variant="ghost"
          size="sm"
          mx={2}
          display={expanded ? 'none' : 'inline-flex'}
        />
        <HStack
          spacing={2}
          ml={2}
          width="100%"
          opacity={expanded ? 1 : 0}
          transition="opacity 0.4s cubic-bezier(0.4,0,0.2,1)"
          pointerEvents={contentVisible ? 'auto' : 'none'}
          style={{ display: contentVisible ? 'flex' : 'none' }}
        >
          <SearchEntities />
          <DrawerFilterToolbox />
          <IconButton
            data-testid="toolbox-search-collapse"
            aria-label={t('workspace.controls.collapse')}
            icon={<Icon as={ChevronRightIcon} boxSize="24px" />}
            onClick={() => setExpanded(false)}
            variant="ghost"
            size="sm"
            mr={2}
          />
        </HStack>
      </Box>
    </Panel>
  )
}

export default CanvasToolbar
