import { FC, useState } from 'react'
import { Edge, MarkerType, Node, Panel, useOnSelectionChange } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Icon, useTheme } from '@chakra-ui/react'
import { ImMakeGroup } from 'react-icons/im'

import { Adapter, Status } from '@/api/__generated__'
import { EdgeTypes, Group, IdStubs, NodeTypes } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { getThemeForStatus } from '../../utils/status-utils.ts'
import { getGroupLayout } from '../../utils/group.utils.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'

const GroupNodesControl: FC = () => {
  const { t } = useTranslation()
  const { onInsertGroupNode } = useWorkspaceStore()
  const [currentSelection, setCurrentSelection] = useState<Node[]>([])
  const theme = useTheme()

  useOnSelectionChange({
    onChange: ({ nodes }) => {
      if (nodes.length >= 2)
        setCurrentSelection(() => nodes.filter((e) => e.type === NodeTypes.ADAPTER_NODE && e.parentNode === undefined))
      else setCurrentSelection([])
    },
  })

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
