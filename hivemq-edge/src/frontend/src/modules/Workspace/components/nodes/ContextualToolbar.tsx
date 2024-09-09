import { type FC, MouseEventHandler, useMemo } from 'react'
import { NodeToolbar, type NodeProps, type NodeToolbarProps, Position, Node, Edge, MarkerType } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { useTheme } from '@chakra-ui/react'
import { LuPanelRightOpen } from 'react-icons/lu'
import { ImMakeGroup } from 'react-icons/im'

import { Adapter, Status } from '@/api/__generated__'
import { EdgeTypes, Group, IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { getGroupLayout } from '@/modules/Workspace/utils/group.utils.ts'
import { getThemeForStatus } from '@/modules/Workspace/utils/status-utils.ts'
import WorkspaceButtonGroup from '@/modules/Workspace/components/parts/WorkspaceButtonGroup.tsx'

type SelectedNodeProps = Pick<NodeProps, 'id' | `dragging`> & Pick<NodeToolbarProps, 'position'>
interface ContextualToolbarProps extends SelectedNodeProps {
  onOpenPanel?: MouseEventHandler | undefined
  children?: React.ReactNode
}

const ContextualToolbar: FC<ContextualToolbarProps> = ({ id, onOpenPanel, children, dragging }) => {
  const { t } = useTranslation()
  const { onInsertGroupNode, nodes } = useWorkspaceStore()
  const theme = useTheme()

  const selectedNodes = nodes.filter((node) => node.selected)
  const selectedGroupCandidates = useMemo(() => {
    // TODO[NVL] Should the grouping only be available if ALL nodes match the filter ?
    const adapters = selectedNodes.filter(
      (node) => node.type === NodeTypes.ADAPTER_NODE && !node.parentId && !node.parentNode
    )
    return adapters.length >= 2 ? adapters : undefined
  }, [selectedNodes])

  const onCreateGroup = () => {
    if (!selectedGroupCandidates) return

    const groupId = `${IdStubs.GROUP_NODE}@${selectedGroupCandidates.map((node) => node.data.id).join('+')}`
    const groupTitle = t('workspace.grouping.untitled')
    const rect = getGroupLayout(selectedGroupCandidates)
    const newGroupNode: Node<Group, NodeTypes.CLUSTER_NODE> = {
      id: groupId,
      type: NodeTypes.CLUSTER_NODE,
      data: {
        childrenNodeIds: selectedGroupCandidates.map((node) => node.id),
        title: groupTitle,
        isOpen: true,
        colorScheme: 'red',
      },
      style: {
        width: rect.width,
        height: rect.height,
      },
      position: { x: rect.x, y: rect.y },
    }

    const groupStatus: Status = {
      runtime: selectedGroupCandidates.every(
        (node: Node<Adapter>) => node.data.status?.runtime === Status.runtime.STARTED
      )
        ? Status.runtime.STARTED
        : Status.runtime.STOPPED,
      connection: selectedGroupCandidates.every(
        (node: Node<Adapter>) => node.data.status?.connection === Status.connection.CONNECTED
      )
        ? Status.connection.CONNECTED
        : Status.connection.DISCONNECTED,
    }

    const newAEdge: Edge = {
      id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${groupId}`,
      target: IdStubs.EDGE_NODE,
      targetHandle: 'Top',
      source: groupId,
      hidden: true,
      focusable: false,
      type: EdgeTypes.REPORT_EDGE,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: getThemeForStatus(theme, groupStatus),
      },
      animated: groupStatus.connection === Status.connection.CONNECTED,
      style: {
        strokeWidth: 1.5,
        stroke: getThemeForStatus(theme, groupStatus),
      },
    }

    onInsertGroupNode(newGroupNode, newAEdge, rect)
  }

  // TODO[NVL] Weird side effect if first node has no toolbar; get the first suitable node instead?
  const [mainNodes] = selectedNodes

  const isGroupable = selectedGroupCandidates

  return (
    <>
      <NodeToolbar
        isVisible={Boolean(mainNodes?.id === id && !dragging)}
        position={Position.Right}
        role="toolbar"
        aria-label={t('workspace.toolbar.container.right')}
      >
        <WorkspaceButtonGroup>
          <IconButton
            size="sm"
            data-testid="node-group-toolbar-panel"
            icon={<LuPanelRightOpen />}
            aria-label={t('workspace.toolbar.command.overview')}
            onClick={onOpenPanel}
          />
        </WorkspaceButtonGroup>
      </NodeToolbar>
      {(children || isGroupable) && (
        <NodeToolbar
          isVisible={Boolean(mainNodes?.id === id && !dragging)}
          position={Position.Left}
          role="toolbar"
          aria-label={t('workspace.toolbar.container.left')}
          style={{ display: 'flex', gap: '4px', flexDirection: 'column' }}
        >
          {children}
          {isGroupable && (
            <WorkspaceButtonGroup>
              <IconButton
                data-testid="node-group-toolbar-group"
                icon={<ImMakeGroup />}
                aria-label={t('workspace.toolbar.command.group')}
                onClick={onCreateGroup}
              />
            </WorkspaceButtonGroup>
          )}
        </NodeToolbar>
      )}
    </>
  )
}

export default ContextualToolbar
