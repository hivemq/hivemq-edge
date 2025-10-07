import type { FC } from 'react'
import { HStack } from '@chakra-ui/react'

import Panel from '@/components/react-flow/Panel.tsx'
import SearchEntities from '@/modules/Workspace/components/filters/SearchEntities.tsx'

const CanvasToolbar: FC = () => {
  return (
    <Panel position="top-left">
      <HStack m={2}>
        <SearchEntities />
      </HStack>
    </Panel>
  )
}

export default CanvasToolbar
