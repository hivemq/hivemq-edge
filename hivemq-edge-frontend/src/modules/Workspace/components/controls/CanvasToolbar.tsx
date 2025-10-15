import type { FC } from 'react'
import { HStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import Panel from '@/components/react-flow/Panel.tsx'
import SearchEntities from '@/modules/Workspace/components/filters/SearchEntities.tsx'
import DrawerFilterToolbox from '@/modules/Workspace/components/filters/DrawerFilterToolbox.tsx'

const CanvasToolbar: FC = () => {
  const { t } = useTranslation()
  return (
    <Panel
      position="top-right"
      data-testid="content-toolbar"
      role="group"
      aria-label={t('workspace.canvas.toolbar.search-filter')}
    >
      <HStack>
        <SearchEntities />
        <DrawerFilterToolbox />
      </HStack>
    </Panel>
  )
}

export default CanvasToolbar
