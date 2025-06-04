import { type FC } from 'react'
import ReactTreeView, { flattenTree } from 'react-accessible-treeview'
import { Box, HStack, Icon, Text } from '@chakra-ui/react'
import { LuChevronDown, LuChevronRight } from 'react-icons/lu'

import { Topic } from '@/components/MQTT/EntityTag.tsx'
import { type SunburstData } from 'recharts/types/chart/SunburstChart'

interface TreeViewProps {
  data: SunburstData
  onSelect?: (topic: string) => void
}

// TODO[THEME] Import from the ChakraUI Theme
const THEME_VISUAL_TABULATOR_SPACING = 8

const TreeViewChart: FC<TreeViewProps> = ({ data }) => {
  return (
    <ReactTreeView
      data={flattenTree(data)}
      nodeRenderer={({ element, getNodeProps, level, isBranch, isExpanded }) => {
        return (
          <HStack {...getNodeProps()} marginLeft={THEME_VISUAL_TABULATOR_SPACING * (level - 1)}>
            {isBranch && (
              <>
                <Icon as={isExpanded ? LuChevronDown : LuChevronRight} />
                <Text>{element.id}</Text>
              </>
            )}
            {!isBranch && (
              <Box m={1}>
                <Topic tagTitle={element.id.toString().substring(1)} />
              </Box>
            )}
          </HStack>
        )
      }}
    />
  )
}

export default TreeViewChart
