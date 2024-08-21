import { FC, useEffect, useState } from 'react'
import { Edge, MarkerType, Node, Panel } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Icon, useTheme } from '@chakra-ui/react'
import { ImMakeGroup } from 'react-icons/im'

import { Adapter, Status } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { EdgeTypes, Group, IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'
import { getGroupLayout } from '@/modules/Workspace/utils/group.utils.ts'
import { getThemeForStatus } from '@/modules/Workspace/utils/status-utils.ts'

/**
 * @deprecated Not in used anymore
 */
const GroupNodesControl: FC = () => {
  const { t } = useTranslation()
  const { onInsertGroupNode, nodes } = useWorkspaceStore()
  const [currentSelection, setCurrentSelection] = useState<Node[]>([])
  const theme = useTheme()

  useEffect(() => {
    const selectedNodes = nodes.filter((node) => node.selected)
    if (selectedNodes.length >= 2)
      setCurrentSelection(() =>
        selectedNodes.filter((node) => node.type === NodeTypes.ADAPTER_NODE && node.parentNode === undefined)
      )
    else setCurrentSelection([])
  }, [nodes])

  const onCreateGroup = () => {
    const groupId = `${IdStubs.GROUP_NODE}@${currentSelection.map((e) => e.data.id).join('+')}`
    const groupTitle = t('workspace.grouping.untitled')
    const rect = getGroupLayout(currentSelection)
    const newGroupNode: Node<Group, NodeTypes.CLUSTER_NODE> = {
      id: groupId,
      type: NodeTypes.CLUSTER_NODE,
      data: { childrenNodeIds: currentSelection.map((e) => e.id), title: groupTitle, isOpen: true, colorScheme: 'red' },
      style: {
        width: rect.width,
        height: rect.height,
      },
      position: { x: rect.x, y: rect.y },
    }

    const groupStatus: Status = {
      runtime: currentSelection.every((e: Node<Adapter>) => e.data.status?.runtime === Status.runtime.STARTED)
        ? Status.runtime.STARTED
        : Status.runtime.STOPPED,
      connection: currentSelection.every(
        (e: Node<Adapter>) => e.data.status?.connection !== Status.connection.CONNECTED
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
    setCurrentSelection([])
  }

  // if (currentSelection.length < 2) return null
  return (
    <Panel position="top-left">
      <IconButton
        icon={<Icon as={ImMakeGroup} boxSize={5} />}
        aria-label={t('workspace.controls.group')}
        onClick={onCreateGroup}
        isDisabled={currentSelection.length < 2}
      />
    </Panel>
  )
}

export default GroupNodesControl
