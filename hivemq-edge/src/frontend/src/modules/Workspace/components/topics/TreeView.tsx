import { type FC } from 'react'
import ReactTreeView, { flattenTree } from 'react-accessible-treeview'
import { Box, HStack, Icon, Text } from '@chakra-ui/react'
import { LuChevronDown, LuChevronRight } from 'react-icons/lu'

import Topic from '@/components/MQTT/Topic.tsx'
import { type SunburstData } from 'recharts/types/chart/SunburstChart'

interface TreeViewProps {
  data: SunburstData
  onSelect?: (topic: string) => void
}

const TreeView: FC<TreeViewProps> = ({ data }) => {
  return (
    <ReactTreeView
      data={flattenTree(data)}
      nodeRenderer={({ element, getNodeProps, level, isBranch, isExpanded }) => {
        return (
          <HStack {...getNodeProps()} marginLeft={8 * (level - 1)}>
            {isBranch && (
              <>
                <Icon as={isExpanded ? LuChevronDown : LuChevronRight} />
                <Text>{element.id}</Text>
              </>
            )}
            {!isBranch && (
              <Box m={1}>
                <Topic topic={element.id.toString().substring(1)} />
              </Box>
            )}
          </HStack>
        )
      }}
    />
  )
}

export default TreeView
