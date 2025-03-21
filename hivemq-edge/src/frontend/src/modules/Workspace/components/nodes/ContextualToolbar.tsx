import type { MouseEventHandler } from 'react'
import { type FC, useMemo } from 'react'
import type { Edge, Node } from 'reactflow'
import { getOutgoers, MarkerType, type NodeProps, type NodeToolbarProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Divider, Text, useTheme, useToast } from '@chakra-ui/react'
import { LuPanelRightOpen } from 'react-icons/lu'
import { ImMakeGroup } from 'react-icons/im'
import { MdScheduleSend } from 'react-icons/md'
import { v4 as uuidv4 } from 'uuid'

import type { Adapter, Bridge, Combiner, EntityReference } from '@/api/__generated__'
import { EntityType, Status } from '@/api/__generated__'
import { useCreateCombiner } from '@/api/hooks/useCombiners/useCreateCombiner'

import IconButton from '@/components/Chakra/IconButton.tsx'
import NodeToolbar from '@/components/react-flow/NodeToolbar.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import type { Group } from '@/modules/Workspace/types.ts'
import { EdgeTypes, IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { getGroupLayout } from '@/modules/Workspace/utils/group.utils.ts'
import { getThemeForStatus } from '@/modules/Workspace/utils/status-utils.ts'
import { gluedNodeDefinition } from '@/modules/Workspace/utils/nodes-utils.ts'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'

// TODO[NVL] Should the grouping only be available if ALL nodes match the filter ?
type CombinerEligibleNode = Node<Adapter, NodeTypes.ADAPTER_NODE> | Node<Bridge, NodeTypes.BRIDGE_NODE>

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
  const createCombiner = useCreateCombiner()
  // TODO[30429] Need a workspace-wide feedback mechanism
  const toast = useToast(BASE_TOAST_OPTION)

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

  const selectedCombinerCandidates = useMemo(() => {
    // TODO[29097] Decide which entities are eligible for combining. Hidden is EDGE, default are ADAPTER and BRIDGE;
    const result = selectedNodes.filter(
      (node) => node.type === NodeTypes.ADAPTER_NODE || node.type === NodeTypes.BRIDGE_NODE
    ) as CombinerEligibleNode[]
    return result.length ? result : undefined
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

  const onManageOrchestrators = () => {
    if (!selectedCombinerCandidates) return
    const edgeNode = nodes.find((node) => node.type === NodeTypes.EDGE_NODE)
    if (!edgeNode) return

    const newOrchestratorNodeId = uuidv4()
    const links = selectedCombinerCandidates.map<EntityReference>((node) => {
      const entity: EntityReference = {
        type: node.type === NodeTypes.ADAPTER_NODE ? EntityType.ADAPTER : EntityType.BRIDGE,
        id: node.data.id,
      }

      return entity
    })

    const newCombiner: Combiner = {
      id: newOrchestratorNodeId,
      name: t('combiner.unnamed'),
      sources: { items: links },
      mappings: { items: [] },
    }

    toast.promise(createCombiner.mutateAsync({ requestBody: newCombiner }), {
      success: { title: t('combiner.toast.create.title'), description: t('combiner.toast.create.success') },
      error: { title: t('combiner.toast.create.title'), description: t('combiner.toast.create.error') },
      loading: { title: t('combiner.toast.create.title'), description: t('combiner.toast.loading') },
    })
  }

  // TODO[NVL] Weird side effect if first node has no toolbar; get the first suitable node instead?
  const [mainNodes] = selectedNodes

  return (
    <NodeToolbar
      isVisible={Boolean(mainNodes?.id === id && !dragging)}
      position={Position.Top}
      aria-label={t('workspace.toolbar.container.label')}
    >
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
          isDisabled={!selectedGroupCandidates}
          data-testid="node-group-toolbar-group"
          icon={<ImMakeGroup />}
          aria-label={t('workspace.toolbar.command.group')}
          onClick={onCreateGroup}
        />
      </ToolbarButtonGroup>

      <ToolbarButtonGroup>
        <IconButton
          isDisabled={!selectedCombinerCandidates}
          data-testid="node-group-toolbar-combiner"
          icon={<MdScheduleSend />}
          aria-label={t('workspace.toolbar.command.combiner.create')}
          onClick={onManageOrchestrators}
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
    </NodeToolbar>
  )
}

export default ContextualToolbar
