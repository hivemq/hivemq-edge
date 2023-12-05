import { FC } from 'react'
import { Handle, NodeProps, NodeResizer, NodeToolbar, Position } from 'reactflow'
import { useNavigate } from 'react-router-dom'
import { Box, Button, Text, useTheme, VStack } from '@chakra-ui/react'

import { Group } from '../../types.ts'
import useWorkspaceStore from '../../utils/store.ts'
import { useTranslation } from 'react-i18next'

const NodeGroup: FC<NodeProps<Group>> = ({ id, data, selected }) => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { colors } = useTheme()
  const { onToggleGroup } = useWorkspaceStore()

  return (
    <>
      <NodeToolbar isVisible={selected} position={Position.Top}>
        <VStack alignItems={'flex-start'}>
          <Button
            size={'xs'}
            onClick={() => {
              data.isOpen = !data.isOpen
              onToggleGroup({ id, data }, data.isOpen)
            }}
          >
            {data.isOpen ? t('workspace.grouping.command.close') : t('workspace.grouping.command.open')}
          </Button>
        </VStack>
      </NodeToolbar>
      <NodeResizer isVisible={true} minWidth={180} minHeight={100} />

      <Box
        w={'100%'}
        h={'100%'}
        style={{
          backgroundColor: data.isOpen ? undefined : colors.red[50],
          // borderRadius: '100%',
          // opacity: 0.05,
          // borderColor: 'red',
          borderWidth: 3,
          borderStyle: 'solid',
        }}
        onDoubleClick={() => navigate(`/edge-flow/group/${id}`)}
      >
        <Text m={2} color={'blackAlpha.900'}>
          {data.title}
        </Text>
      </Box>
      <Handle type="source" position={Position.Bottom} id="a" />
    </>
  )
}

export default NodeGroup
