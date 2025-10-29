import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type {
  NodeProps,
  NodePositionChange,
  NodeRemoveChange,
  NodeReplaceChange,
  EdgeRemoveChange,
} from '@xyflow/react'
import { Handle, NodeResizer, Position, useReactFlow, useNodesData } from '@xyflow/react'
import { Box, Icon, Text, useColorMode, useDisclosure, useTheme } from '@chakra-ui/react'
import { LuExpand, LuShrink } from 'react-icons/lu'
import { ImUngroup } from 'react-icons/im'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import type { NodeGroupType } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { useContextMenu } from '../../hooks/useContextMenu.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'

const NodeGroup: FC<NodeProps<NodeGroupType>> = ({ id, data, selected, ...props }) => {
  const { t } = useTranslation()
  const { colors } = useTheme()
  const { onToggleGroup, onNodesChange, onEdgesChange, nodes, edges } = useWorkspaceStore()
  const { isOpen: isConfirmUngroupOpen, onOpen: onConfirmUngroupOpen, onClose: onConfirmUngroupClose } = useDisclosure()
  const { onContextMenu } = useContextMenu(id, selected, `/workspace/group/${id}`)
  const { colorMode } = useColorMode()
  const isLight = colorMode === 'light'
  const { updateNodeData } = useReactFlow()

  // Use React Flow's efficient hook to get child node data directly
  const childNodesData = useNodesData(data.childrenNodeIds)

  // Compute unified status model - aggregates from child nodes using React Flow's optimized hook
  const statusModel = useMemo(() => {
    let hasErrorRuntime = false
    let hasActiveRuntime = false
    let hasErrorOperational = false
    let hasActiveOperational = false

    // Aggregate status from all child nodes
    for (const child of childNodesData) {
      if (!child) continue
      const childStatusModel = (child.data as { statusModel?: NodeStatusModel }).statusModel
      if (!childStatusModel) continue

      if (childStatusModel.runtime === RuntimeStatus.ERROR) hasErrorRuntime = true
      else if (childStatusModel.runtime === RuntimeStatus.ACTIVE) hasActiveRuntime = true

      if (childStatusModel.operational === OperationalStatus.ERROR) hasErrorOperational = true
      else if (childStatusModel.operational === OperationalStatus.ACTIVE) hasActiveOperational = true
    }

    // Determine aggregated runtime status
    const runtime = hasErrorRuntime
      ? RuntimeStatus.ERROR
      : hasActiveRuntime
        ? RuntimeStatus.ACTIVE
        : RuntimeStatus.INACTIVE

    // Determine aggregated operational status
    const operational = hasErrorOperational
      ? OperationalStatus.ERROR
      : hasActiveOperational
        ? OperationalStatus.ACTIVE
        : OperationalStatus.INACTIVE

    return {
      runtime,
      operational,
      source: 'DERIVED' as const,
    }
  }, [childNodesData])

  // Update node data with statusModel whenever it changes
  useEffect(() => {
    updateNodeData(id, { statusModel })
  }, [id, statusModel, updateNodeData])

  const onConfirmUngroup = () => {
    onConfirmUngroupOpen()
  }

  const handleToggle = () => {
    data.isOpen = !data.isOpen
    onToggleGroup({ id, data }, data.isOpen)
  }

  const handleUngroup = () => {
    const content = nodes.filter((node) => data.childrenNodeIds.includes(node.id))
    const changeContent = content.map<NodeReplaceChange>((node) => ({
      id: node.id,
      item: { ...node, parentId: undefined },
      type: 'replace',
    }))
    const changePosition = content.map<NodePositionChange>((node) => ({
      id: node.id,
      position: { x: node.position.x + props.positionAbsoluteX, y: node.position.y + props.positionAbsoluteY },
      type: 'position',
    }))

    onToggleGroup({ id, data }, true)
    onNodesChange(changeContent)
    onNodesChange(changePosition)
    onNodesChange([{ id, type: 'remove' } as NodeRemoveChange])
    onEdgesChange(
      edges.filter((edge) => edge.source === id).map((e) => ({ id: e.id, type: 'remove' }) as EdgeRemoveChange)
    )
  }

  return (
    <>
      <ContextualToolbar id={id} title={data.title} dragging={props.dragging} onOpenPanel={onContextMenu}>
        <ToolbarButtonGroup isAttached={false}>
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
        </ToolbarButtonGroup>
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
        data-groupopen={data.isOpen}
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
