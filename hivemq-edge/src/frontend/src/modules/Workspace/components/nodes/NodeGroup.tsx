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
import { Box, Button, ButtonGroup, Icon, Text, useColorMode, useDisclosure, useTheme } from '@chakra-ui/react'
import { LuPanelRightOpen } from 'react-icons/lu'
import { ImUngroup } from 'react-icons/im'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import { Group } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { useContextMenu } from '../../hooks/useContextMenu.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

const NodeGroup: FC<NodeProps<Group>> = ({ id, data, selected, ...props }) => {
  const { t } = useTranslation()
  const { colors } = useTheme()
  const { onToggleGroup, onNodesChange, onEdgesChange, nodes, edges } = useWorkspaceStore()
  const { isOpen: isConfirmUngroupOpen, onOpen: onConfirmUngroupOpen, onClose: onConfirmUngroupClose } = useDisclosure()
  const { onContextMenu } = useContextMenu(id, selected, '/edge-flow/group')
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
      <NodeToolbar
        isVisible={selected}
        position={Position.Top}
        role={'toolbar'}
        aria-controls={`node-group-${id}`}
        aria-label={t('workspace.grouping.toolbar.aria-label', { id }) as string}
        style={{ display: 'flex', gap: '12px' }}
      >
        <ButtonGroup size="sm" isAttached variant="outline" colorScheme={'gray'}>
          <Button data-testid={'node-group-toolbar-expand'} onClick={handleToggle}>
            {!data.isOpen ? t('workspace.grouping.command.expand') : t('workspace.grouping.command.collapse')}
          </Button>
          <IconButton
            data-testid={'node-group-toolbar-ungroup'}
            icon={<Icon as={ImUngroup} boxSize={5} />}
            aria-label={t('workspace.grouping.command.ungroup')}
            onClick={onConfirmUngroup}
          />
        </ButtonGroup>
        <IconButton
          size="sm"
          variant="solid"
          colorScheme={'gray'}
          data-testid={'node-group-toolbar-panel'}
          icon={<Icon as={LuPanelRightOpen} boxSize={5} />}
          aria-label={t('workspace.grouping.command.overview')}
          onClick={onContextMenu}
        />
      </NodeToolbar>
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
        w={'100%'}
        h={'100%'}
        backgroundColor={
          data.isOpen ? undefined : data.colorScheme ? colors[data.colorScheme][isLight ? 50 : 900] : colors.red[50]
        }
        borderColor={data.colorScheme ? colors[data.colorScheme][500] : colors.red[50]}
        borderWidth={1}
        borderStyle={'solid'}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
      >
        <Text m={2} colorScheme={'black'}>
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
