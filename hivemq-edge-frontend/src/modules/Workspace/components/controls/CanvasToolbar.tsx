import type { FC } from 'react'
import { HStack } from '@chakra-ui/react'

import Panel from '@/components/react-flow/Panel.tsx'
import SearchEntities from '@/modules/Workspace/components/filters/SearchEntities.tsx'
import DrawerFilterToolbox from '@/modules/Workspace/components/filters/DrawerFilterToolbox.tsx'

const CanvasToolbar: FC = () => {
  return (
    <Panel position="top-right">
      <HStack m={2}>
        <SearchEntities />
        <DrawerFilterToolbox />
      </HStack>
    </Panel>
  )
}

export default CanvasToolbar
