import { FC, useState } from 'react'
import { Edge, MarkerType, Node, Panel, useOnSelectionChange } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { IconButton, useTheme } from '@chakra-ui/react'
import { GrObjectGroup } from 'react-icons/gr'

import { Status } from '@/api/__generated__'
import { EdgeTypes, Group, IdStubs, NodeTypes } from '../../types.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore.ts'
import { getThemeForStatus } from '../../utils/status-utils.ts'

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
    const newGroupNode: Node<Group, NodeTypes.CLUSTER_NODE> = {
      id: groupId,
      type: NodeTypes.CLUSTER_NODE,
      data: { childrenNodeIds: currentSelection.map((e) => e.id), title: groupTitle, isOpen: true },
      // TODO[NVL] Position/size needs to be more elegant, including adjustment of children nodes
      position: { x: 20, y: 20 },
      style: {
        width: 500,
        height: 140,
      },
    }

    const newAEdge: Edge = {
      id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${groupId}`,
      target: IdStubs.EDGE_NODE,
      targetHandle: 'Top',
      source: groupId,
      hidden: true,
      type: EdgeTypes.REPORT_EDGE,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: getThemeForStatus(theme, { connection: Status.connection.CONNECTED, runtime: Status.runtime.STARTED }),
      },
      animated: true,
      style: {
        strokeWidth: 1.5,
        stroke: getThemeForStatus(theme, { connection: Status.connection.CONNECTED, runtime: Status.runtime.STARTED }),
      },
    }

    onInsertGroupNode(newGroupNode, newAEdge)
    setCurrentSelection([])
  }

  if (currentSelection.length < 2) return null
  return (
    <Panel position="top-left">
      <IconButton
        icon={<GrObjectGroup />}
        aria-label={t('workspace.grouping.create')}
        onClick={onCreateGroup}
        isDisabled={currentSelection.length < 2}
      />
    </Panel>
  )
}

export default GroupNodesControl
