import type { MouseEventHandler, FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { v4 as uuidv4 } from 'uuid'
import type { Node, NodeProps, NodeToolbarProps } from '@xyflow/react'
import { Position, getOutgoers, useStore, useReactFlow } from '@xyflow/react'

import { Divider, Text, useTheme, useToast } from '@chakra-ui/react'
import { ImMakeGroup } from 'react-icons/im'
import { LuPanelRightOpen } from 'react-icons/lu'

import type { Combiner, EntityReference } from '@/api/__generated__'
import { useCreateAssetMapper } from '@/api/hooks/useAssetMapper'
import { useCreateCombiner } from '@/api/hooks/useCombiners'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { HqAssets, HqCombiner } from '@/components/Icons'
import NodeToolbar from '@/components/react-flow/NodeToolbar.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import { BASE_TOAST_OPTION, DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'
import { ANIMATION } from '@/modules/Theme/utils.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import type { NodeAdapterType, NodeDeviceType } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { createGroup, getGroupBounds } from '@/modules/Workspace/utils/group.utils.ts'
import { gluedNodeDefinition } from '@/modules/Workspace/utils/nodes-utils.ts'
import { resetSelectedNodesState } from '@/modules/Workspace/utils/react-flow.utils.ts'
import {
  buildEntityReferencesFromNodes,
  filterCombinerCandidates,
  findExistingCombiner,
  isAssetMapperCombiner,
} from '@/modules/Workspace/utils/toolbar.utils'

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
  const createAssetMapper = useCreateAssetMapper()
  const resetSelectedNodes = useStore(resetSelectedNodesState)

  const toast = useToast(BASE_TOAST_OPTION)
  const { data } = useGetAdapterTypes()

  const navigate = useNavigate()
  const { fitView, getNodesBounds } = useReactFlow()

  const selectedNodes = useMemo(() => {
    return nodes.filter((node) => node.selected)
  }, [nodes])

  const topSelectedNode = useMemo(() => {
    const [firstNode] = selectedNodes.toSorted((a, b) => {
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
    return filterCombinerCandidates(selectedNodes, data?.items)
  }, [data?.items, selectedNodes])

  const isAssetManager = useMemo(() => {
    return isAssetMapperCombiner(selectedCombinerCandidates)
  }, [selectedCombinerCandidates])

  const onCreateGroup = () => {
    if (!selectedGroupCandidates) return
    const rect = getNodesBounds(selectedGroupCandidates)

    const groupRect = getGroupBounds(rect)
    const { newGroupNode, newGroupEdge } = createGroup(selectedGroupCandidates, groupRect, theme)
    onInsertGroupNode(newGroupNode, newGroupEdge, groupRect)
  }

  const getOptions = (areAllCandidatesConnected: boolean) => ({
    success: {
      title: t('combiner.toast.create.title'),
      description: areAllCandidatesConnected
        ? t('combiner.toast.create.success')
        : t('combiner.toast.create.partialSuccess'),
    },
    error: { title: t('combiner.toast.create.title'), description: t('combiner.toast.create.error') },
    loading: { title: t('combiner.toast.create.title'), description: t('combiner.toast.loading') },
  })

  const onCreateAssetMapper = (links: EntityReference[]) => {
    const newOrchestratorNodeId = uuidv4()

    const newAssetMapper: Combiner = {
      id: newOrchestratorNodeId,
      name: t('pulse.mapper.unnamed'),
      sources: { items: links },
      mappings: { items: [] },
    }

    return createAssetMapper.mutateAsync({ requestBody: newAssetMapper })
  }

  const onCreateCombiner = (links: EntityReference[]) => {
    const newOrchestratorNodeId = uuidv4()

    const newCombiner: Combiner = {
      id: newOrchestratorNodeId,
      name: t('combiner.unnamed'),
      sources: { items: links },
      mappings: { items: [] },
    }
    return createCombiner.mutateAsync({ requestBody: newCombiner })
  }

  const onManageTransformationNode = () => {
    if (!selectedCombinerCandidates) return

    // Build entity references from selected nodes
    const entityReferences = buildEntityReferencesFromNodes(selectedCombinerCandidates)

    // Check if a combiner with these exact sources already exists
    const existingCombiner = findExistingCombiner(nodes, entityReferences)

    const areAllCandidatesConnected = selectedNodes.length - selectedCombinerCandidates.length === 0

    if (existingCombiner) {
      toast({
        ...DEFAULT_TOAST_OPTION,
        status: 'info',
        title: t('combiner.toast.create.title'),
        description: t('A combiner already exists. Please add your mappings to it'),
      })
      const node = nodes.find((e) => e.id === existingCombiner.id)
      if (node) {
        fitView({
          nodes: [{ id: existingCombiner.id }],
          padding: 3,
          duration: ANIMATION.FIT_VIEW_DURATION_MS,
        }).then(() => navigate(`combiner/${existingCombiner.id}`))
      }

      return
    }

    const promise = isAssetManager ? onCreateAssetMapper(entityReferences) : onCreateCombiner(entityReferences)

    toast.promise(promise, getOptions(areAllCandidatesConnected))
    promise.then(() => {
      resetSelectedNodes()
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
          icon={isAssetManager ? <HqAssets /> : <HqCombiner />}
          aria-label={
            isAssetManager
              ? t('workspace.toolbar.command.assets.create')
              : t('workspace.toolbar.command.combiner.create')
          }
          onClick={onManageTransformationNode}
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
