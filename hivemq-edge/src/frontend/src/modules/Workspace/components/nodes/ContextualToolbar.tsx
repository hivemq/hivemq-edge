import { type FC, MouseEventHandler, useMemo } from 'react'
import {
  NodeToolbar,
  type NodeProps,
  type NodeToolbarProps,
  Position,
  Node,
  Edge,
  MarkerType,
  getOutgoers,
} from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Divider, HStack, Icon, Text, useTheme } from '@chakra-ui/react'
import { LuPanelRightOpen } from 'react-icons/lu'
import { ImMakeGroup } from 'react-icons/im'
import { BsGripVertical } from 'react-icons/bs'

import { Adapter, Status } from '@/api/__generated__'
import { EdgeTypes, Group, IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { getGroupLayout } from '@/modules/Workspace/utils/group.utils.ts'
import { getThemeForStatus } from '@/modules/Workspace/utils/status-utils.ts'
import { gluedNodeDefinition } from '@/modules/Workspace/utils/nodes-utils.ts'

type SelectedNodeProps = Pick<NodeProps, 'id' | `dragging`> & Pick<NodeToolbarProps, 'position'>
interface ContextualToolbarProps extends SelectedNodeProps {
  onOpenPanel?: MouseEventHandler | undefined
  children?: React.ReactNode
  hasNoOverview?: boolean
  title?: string
}

const ContextualToolbar: FC<ContextualToolbarProps> = ({
  id,
  title,
  onOpenPanel,
  children,
  hasNoOverview = false,
  dragging,
}) => {
  const { t } = useTranslation()
  const { onInsertGroupNode, nodes, edges } = useWorkspaceStore()
  const theme = useTheme()

  const selectedNodes = nodes.filter((node) => node.selected)
  const selectedGroupCandidates = useMemo(() => {
    // TODO[NVL] Should the grouping only be available if ALL nodes match the filter ?
    const adapters = selectedNodes.filter(
      (node) => node.type === NodeTypes.ADAPTER_NODE && !node.parentId && !node.parentNode
    )

    // Add devices to the group
    const devices = adapters.reduce<Node[]>((acc, curr) => {
      const [type] = gluedNodeDefinition[curr.type as NodeTypes]
      if (!type) return acc
      const outgoers = getOutgoers(curr, nodes, edges)
      const gluedNode = outgoers.find((node) => node.type === type)
      if (gluedNode) acc.push(gluedNode)
      return acc
    }, [])

    return adapters.length >= 2 ? [...adapters, ...devices] : undefined
  }, [edges, nodes, selectedNodes])

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
    <NodeToolbar
      isVisible={Boolean(mainNodes?.id === id && !dragging)}
      position={Position.Top}
      role="toolbar"
      aria-label={t('workspace.toolbar.container.label')}
    >
      <HStack
        sx={{
          borderColor: 'var(--chakra-colors-chakra-border-color)',
          _dark: {
            borderWidth: 1,
          },
          // paddingLeft: 2,
          paddingRight: 2,
          borderRadius: 'var(--chakra-radii-md)',
          backgroundColor: 'var(--chakra-colors-chakra-body-bg)',
          boxShadow:
            'rgba(17, 17, 26, 0.1) 0px 4px 16px, rgba(17, 17, 26, 0.1) 0px 8px 24px, rgba(17, 17, 26, 0.1) 0px 16px 56px;',
        }}
        height="50px"
      >
        <Icon as={BsGripVertical} boxSize={7} aria-hidden={true} />
        <Text data-testid="toolbar-title">{title || id}</Text>
        {children && (
          <>
            <Divider orientation="vertical" />
            {children}
          </>
        )}

        <Divider orientation="vertical" />
        <ToolbarButtonGroup>
          <IconButton
            isDisabled={!isGroupable}
            data-testid="node-group-toolbar-group"
            icon={<ImMakeGroup />}
            aria-label={t('workspace.toolbar.command.group')}
            onClick={onCreateGroup}
          />
        </ToolbarButtonGroup>

        {!hasNoOverview && (
          <>
            <Divider orientation="vertical" />
            <ToolbarButtonGroup>
              <IconButton
                data-testid="node-group-toolbar-panel"
                icon={<LuPanelRightOpen />}
                aria-label={t('workspace.toolbar.command.overview')}
                onClick={onOpenPanel}
              />
            </ToolbarButtonGroup>
          </>
        )}
      </HStack>
    </NodeToolbar>
  )
}

export default ContextualToolbar
