import { FC, useState } from 'react'
import { Edge, MarkerType, Node, Panel, useOnSelectionChange } from 'reactflow'
import { IconButton, useTheme } from '@chakra-ui/react'
import { GrObjectGroup } from 'react-icons/gr'
import useWorkspaceStore from '@/modules/EdgeVisualisation/utils/store.ts'
import { EdgeTypes, Group, IdStubs, NodeTypes } from '@/modules/EdgeVisualisation/types.ts'
import { getThemeForStatus } from '@/modules/EdgeVisualisation/utils/status-utils.ts'
import { Status } from '@/api/__generated__'
import { useTranslation } from 'react-i18next'

const GroupNodesControl: FC = () => {
  const { t } = useTranslation()
  const { onInsertNode } = useWorkspaceStore()
  const [currentSelection, setCurrentSelection] = useState<Node[]>([])
  const theme = useTheme()

  useOnSelectionChange({
    onChange: ({ nodes }) => {
      if (nodes.length >= 2) setCurrentSelection(() => nodes)
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

    onInsertNode(newGroupNode, newAEdge)
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
