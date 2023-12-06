import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Handle,
  NodeProps,
  NodeResizer,
  NodeToolbar,
  Position,
  NodeRemoveChange,
  NodeResetChange,
  EdgeRemoveChange,
} from 'reactflow'
import { useNavigate } from 'react-router-dom'
import { Box, HStack, IconButton, Switch, Text, useTheme } from '@chakra-ui/react'
import { GrObjectUngroup } from 'react-icons/gr'

import { Group } from '../../types.ts'
import useWorkspaceStore from '../../utils/store.ts'

const NodeGroup: FC<NodeProps<Group>> = ({ id, data, selected }) => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { colors } = useTheme()
  const { onToggleGroup, onNodesChange, onEdgesChange, nodes, edges } = useWorkspaceStore()

  const handleToggle = () => {
    data.isOpen = !data.isOpen
    onToggleGroup({ id, data }, data.isOpen)
  }

  const handleUngroup = () => {
    onToggleGroup({ id, data }, true)
    onNodesChange(
      nodes.map((e) => {
        if (data.childrenNodeIds.includes(e.id)) {
          // TODO[NVL] Compute position so that nodes don't move on the screen
          return { item: { ...e, parentNode: undefined }, type: 'reset' } as NodeResetChange
        } else return { item: e, type: 'reset' } as NodeResetChange
      })
    )
    onNodesChange([{ id, type: 'remove' } as NodeRemoveChange])
    onEdgesChange(edges.filter((e) => e.source === id).map((e) => ({ id: e.id, type: 'remove' } as EdgeRemoveChange)))
  }

  return (
    <>
      <NodeToolbar isVisible={selected} position={Position.Top}>
        <HStack alignItems={'flex-end'} gap={10}>
          <Switch
            aria-label={t('workspace.grouping.command.expand') as string}
            onChange={handleToggle}
            defaultChecked={!data.isOpen}
          />
          <IconButton
            icon={<GrObjectUngroup />}
            size={'xs'}
            aria-label={t('workspace.grouping.command.ungroup')}
            onClick={handleUngroup}
          />
        </HStack>
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
