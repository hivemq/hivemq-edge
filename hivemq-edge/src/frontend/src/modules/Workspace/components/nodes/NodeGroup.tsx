import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Handle,
  NodeProps,
  NodeResizer,
  Position,
  NodeRemoveChange,
  NodeResetChange,
  EdgeRemoveChange,
} from 'reactflow'
import { Box, ButtonGroup, Icon, Text, useColorMode, useDisclosure, useTheme } from '@chakra-ui/react'
import { LuExpand, LuShrink } from 'react-icons/lu'
import { ImUngroup } from 'react-icons/im'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import { Group } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { useContextMenu } from '../../hooks/useContextMenu.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'

const NodeGroup: FC<NodeProps<Group>> = ({ id, data, selected, ...props }) => {
  const { t } = useTranslation()
  const { colors } = useTheme()
  const { onToggleGroup, onNodesChange, onEdgesChange, nodes, edges } = useWorkspaceStore()
  const { isOpen: isConfirmUngroupOpen, onOpen: onConfirmUngroupOpen, onClose: onConfirmUngroupClose } = useDisclosure()
  const { onContextMenu } = useContextMenu(id, selected, '/workspace/group')
  const { colorMode } = useColorMode()
  const isLight = colorMode === 'light'

  const onConfirmUngroup = () => {
    onConfirmUngroupOpen()
  }

  const handleToggle = () => {
    data.isOpen = !data.isOpen
    onToggleGroup({ id, data }, data.isOpen)
  }

  const handleUngroup = () => {
    // TODO[NVL] Create a store action
    onToggleGroup({ id, data }, true)
    onNodesChange(
      nodes.map((node) => {
        if (data.childrenNodeIds.includes(node.id)) {
          return {
            item: {
              ...node,
              parentNode: undefined,
              position: { x: node.position.x + props.xPos, y: node.position.y + props.yPos },
            },
            type: 'reset',
          } as NodeResetChange
        } else return { item: node, type: 'reset' } as NodeResetChange
      })
    )
    onNodesChange([{ id, type: 'remove' } as NodeRemoveChange])
    onEdgesChange(
      edges.filter((edge) => edge.source === id).map((e) => ({ id: e.id, type: 'remove' } as EdgeRemoveChange))
    )
  }

  return (
    <>
      <ContextualToolbar id={id} onOpenPanel={onContextMenu}>
        <ButtonGroup size="sm" variant="solid" colorScheme="blue" orientation="vertical">
          <IconButton
            data-testid="node-group-toolbar-expand"
            icon={<Icon as={data.isOpen ? LuShrink : LuExpand} boxSize={5} />}
            aria-label={
              !data.isOpen ? t('workspace.grouping.command.expand') : t('workspace.grouping.command.collapse')
            }
            onClick={handleToggle}
          />
          <IconButton
            data-testid="node-group-toolbar-ungroup"
            icon={<Icon as={ImUngroup} boxSize={5} />}
            aria-label={t('workspace.grouping.command.ungroup')}
            onClick={onConfirmUngroup}
          />
        </ButtonGroup>
      </ContextualToolbar>
      {selected && (
        <NodeResizer
          isVisible={true}
          minWidth={180}
          minHeight={100}
          handleStyle={{ width: '8px', height: '8px', backgroundColor: 'transparent', borderColor: 'gray' }}
          lineStyle={{ borderWidth: 5, borderColor: 'transparent' }}
        />
      )}

      <Box
        id={`node-group-${id}`}
        w="100%"
        h="100%"
        backgroundColor={
          data.isOpen ? undefined : data.colorScheme ? colors[data.colorScheme][isLight ? 50 : 900] : colors.red[50]
        }
        borderColor={data.colorScheme ? colors[data.colorScheme][500] : colors.red[50]}
        borderWidth={1}
        borderStyle="solid"
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
      >
        <Text m={2} colorScheme="black">
          {data.title}
        </Text>
      </Box>
      <Handle type="source" position={Position.Bottom} id="a" isConnectable={false} />
      <ConfirmationDialog
        isOpen={isConfirmUngroupOpen}
        onClose={onConfirmUngroupClose}
        onSubmit={handleUngroup}
        message={t('workspace.grouping.modal.description')}
        header={t('workspace.grouping.modal.title')}
      />
    </>
  )
}

export default NodeGroup
