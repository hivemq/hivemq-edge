import type { MouseEventHandler } from 'react'
import { type FC, useMemo } from 'react'
import type { Node } from '@xyflow/react'
import { useReactFlow } from '@xyflow/react'
import { getOutgoers, type NodeProps, type NodeToolbarProps, Position } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Divider, Text, useTheme, useToast } from '@chakra-ui/react'
import { LuPanelRightOpen } from 'react-icons/lu'
import { ImMakeGroup } from 'react-icons/im'
import { v4 as uuidv4 } from 'uuid'
import { useNavigate } from 'react-router-dom'

import type { Adapter, Bridge, Combiner, EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useCreateCombiner, useListCombiners } from '@/api/hooks/useCombiners'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'

import { HqCombiner } from '@/components/Icons'
import IconButton from '@/components/Chakra/IconButton.tsx'
import NodeToolbar from '@/components/react-flow/NodeToolbar.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import { BASE_TOAST_OPTION, DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'
import { ANIMATION } from '@/modules/Theme/utils.ts'
import type { NodeAdapterType, NodeDeviceType } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { createGroup, getGroupBounds } from '@/modules/Workspace/utils/group.utils.ts'
import { gluedNodeDefinition } from '@/modules/Workspace/utils/nodes-utils.ts'
import { arrayWithSameObjects } from '@/modules/Workspace/utils/combiner.utils'

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
  const toast = useToast(BASE_TOAST_OPTION)
  const { data } = useGetAdapterTypes()
  const { data: combiners } = useListCombiners()
  const navigate = useNavigate()
  const { fitView, getNodesBounds } = useReactFlow()

  const selectedNodes = useMemo(() => {
    return nodes.filter((node) => node.selected)
  }, [nodes])

  const topSelectedNode = useMemo(() => {
    const [firstNode] = selectedNodes.sort((a, b) => {
      return a.position.y - b.position.y < 0 ? -1 : 1
    })

    return firstNode
  }, [selectedNodes])

  const selectedGroupCandidates = useMemo(() => {
    // TODO[NVL] Should the grouping only be available if ALL nodes match the filter ?
    const adapters = selectedNodes.filter(
      (node) => node.type === NodeTypes.ADAPTER_NODE && !node.parentId && !node.parentId
    ) as NodeAdapterType[]

    // Add devices to the group
    const devices = adapters.reduce<Node[]>((acc, curr) => {
      const [type] = gluedNodeDefinition[curr.type as NodeTypes]
      if (!type) return acc
      const outgoers = getOutgoers(curr, nodes, edges)
      const gluedNode = outgoers.find((node) => node.type === type)
      if (gluedNode) acc.push(gluedNode)
      return acc
    }, []) as NodeDeviceType[]

    return adapters.length >= 2 ? [...adapters, ...devices] : undefined
  }, [edges, nodes, selectedNodes])

  const selectedCombinerCandidates = useMemo(() => {
    const result = selectedNodes.filter((node) => {
      if (node.type === NodeTypes.ADAPTER_NODE) {
        const protocol = data?.items.find((e) => e.id === node.data.type)
        return protocol?.capabilities?.includes('COMBINE')
      }

      return node.type === NodeTypes.BRIDGE_NODE
    }) as CombinerEligibleNode[]
    return result.length ? result : undefined
  }, [data?.items, selectedNodes])

  const onCreateGroup = () => {
    if (!selectedGroupCandidates) return
    const rect = getNodesBounds(selectedGroupCandidates)

    const groupRect = getGroupBounds(rect)
    const { newGroupNode, newGroupEdge } = createGroup(selectedGroupCandidates, groupRect, theme)

    onInsertGroupNode(newGroupNode, newGroupEdge, groupRect)
  }

  const onManageCombiners = () => {
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

    const isCombinerAlreadyDefined = combiners?.items?.find((e) =>
      arrayWithSameObjects<EntityReference>(links)(e.sources.items)
    )

    const isCombinerSourcesAllValid = selectedNodes.length - selectedCombinerCandidates.length === 0

    if (isCombinerAlreadyDefined) {
      toast({
        ...DEFAULT_TOAST_OPTION,
        status: 'info',
        title: t('combiner.toast.create.title'),
        description: t('A combiner already exists. Please add your mappings to it'),
      })
      const node = nodes.find((e) => e.id === isCombinerAlreadyDefined.id)
      if (node) {
        fitView({ nodes: [{ id: isCombinerAlreadyDefined.id }], padding: 3, duration: ANIMATION.FIT_VIEW_DURATION_MS })
        navigate(`combiner/${isCombinerAlreadyDefined.id}`)
      }

      return
    }

    const newCombiner: Combiner = {
      id: newOrchestratorNodeId,
      name: t('combiner.unnamed'),
      sources: { items: links },
      mappings: { items: [] },
    }

    toast.promise(createCombiner.mutateAsync({ requestBody: newCombiner }), {
      success: {
        title: t('combiner.toast.create.title'),
        description: isCombinerSourcesAllValid
          ? t('combiner.toast.create.success')
          : t('combiner.toast.create.partialSuccess'),
      },
      error: { title: t('combiner.toast.create.title'), description: t('combiner.toast.create.error') },
      loading: { title: t('combiner.toast.create.title'), description: t('combiner.toast.loading') },
    })
  }

  const isMultiple = selectedNodes.length >= 2

  return (
    <NodeToolbar
      isVisible={Boolean(topSelectedNode?.id === id && !dragging)}
      position={Position.Top}
      aria-label={t('workspace.toolbar.container.label')}
    >
      <Text data-testid="toolbar-title">
        {isMultiple ? t('workspace.toolbar.selection.title', { count: selectedNodes.length }) : title || id}
      </Text>
      {children && !isMultiple && (
        <>
          <Divider orientation="vertical" />
          {children}
        </>
      )}
      <ToolbarButtonGroup>
        <IconButton
          isDisabled={!selectedCombinerCandidates}
          data-testid="node-group-toolbar-combiner"
          icon={<HqCombiner />}
          aria-label={t('workspace.toolbar.command.combiner.create')}
          onClick={onManageCombiners}
        />
      </ToolbarButtonGroup>

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

      {!hasNoOverview && !isMultiple && (
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
