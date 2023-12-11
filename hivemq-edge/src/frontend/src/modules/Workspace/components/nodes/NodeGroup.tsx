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
import { Box, Button, ButtonGroup, Icon, IconButton, Text, useDisclosure, useTheme } from '@chakra-ui/react'
import { GrObjectUngroup } from 'react-icons/gr'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import { Group } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'

const NodeGroup: FC<NodeProps<Group>> = ({ id, data, selected, ...props }) => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { colors } = useTheme()
  const { onToggleGroup, onNodesChange, onEdgesChange, nodes, edges } = useWorkspaceStore()
  const { isOpen: isConfirmUngroupOpen, onOpen: onConfirmUngroupOpen, onClose: onConfirmUngroupClose } = useDisclosure()

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
      nodes.map((e) => {
        if (data.childrenNodeIds.includes(e.id)) {
          return {
            item: {
              ...e,
              parentNode: undefined,
              position: { x: e.position.x + props.xPos, y: e.position.y + props.yPos },
            },
            type: 'reset',
          } as NodeResetChange
        } else return { item: e, type: 'reset' } as NodeResetChange
      })
    )
    onNodesChange([{ id, type: 'remove' } as NodeRemoveChange])
    onEdgesChange(edges.filter((e) => e.source === id).map((e) => ({ id: e.id, type: 'remove' } as EdgeRemoveChange)))
  }

  return (
    <>
      <NodeToolbar isVisible={selected} position={Position.Top}>
        <ButtonGroup
          size="sm"
          isAttached
          variant="outline"
          aria-controls={`node-group-${id}`}
          aria-label={t('workspace.grouping.toolbar.aria-label', { id }) as string}
        >
          <Button data-testid={'node-group-toolbar-expand'} onClick={handleToggle}>
            {!data.isOpen ? t('workspace.grouping.command.expand') : t('workspace.grouping.command.collapse')}
          </Button>
          <IconButton
            data-testid={'node-group-toolbar-ungroup'}
            icon={<Icon as={GrObjectUngroup} />}
            aria-label={t('workspace.grouping.command.ungroup')}
            onClick={onConfirmUngroup}
          />
        </ButtonGroup>
      </NodeToolbar>
      {selected && <NodeResizer isVisible={true} minWidth={180} minHeight={100} />}

      <Box
        id={`node-group-${id}`}
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
