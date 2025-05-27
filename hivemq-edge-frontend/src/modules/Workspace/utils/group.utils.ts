import type { Edge, Node, Rect } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'
import type { Dict } from '@chakra-ui/utils'
import type { WithCSSVar } from '@chakra-ui/react'

import { Status } from '@/api/__generated__'
import type { NodeAdapterType, NodeDeviceType } from '@/modules/Workspace/types'
import { EdgeTypes, type Group, IdStubs, NodeTypes } from '@/modules/Workspace/types'
import { getThemeForStatus } from './status-utils'

import i18n from '@/config/i18n.config.ts'

const GROUP_MARGIN = 20
const GROUP_TITLE_MARGIN = 24

export const getGroupBounds = (rect: Rect): Rect => {
  return {
    x: rect.x - GROUP_MARGIN,
    y: rect.y - GROUP_MARGIN - GROUP_TITLE_MARGIN,
    width: rect.width + 2 * GROUP_MARGIN,
    height: rect.height + 2 * GROUP_MARGIN + GROUP_TITLE_MARGIN,
  }
}

export const createGroup = (
  selectedGroupCandidates: (NodeAdapterType | NodeDeviceType)[],
  rect: Rect,
  theme: Partial<WithCSSVar<Dict>>
) => {
  const groupId = `${IdStubs.GROUP_NODE}@${selectedGroupCandidates.map((node) => node.data.id).join('+')}`
  const groupTitle = i18n.t('workspace.grouping.untitled')
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
    runtime: selectedGroupCandidates.every((node) => {
      if ('status' in node.data) return node.data.status?.runtime === Status.runtime.STARTED
      return false
    })
      ? Status.runtime.STARTED
      : Status.runtime.STOPPED,
    connection: selectedGroupCandidates.every((node) => {
      if ('status' in node.data) return node.data.status?.connection === Status.connection.CONNECTED
      return false
    })
      ? Status.connection.CONNECTED
      : Status.connection.DISCONNECTED,
  }

  const newGroupEdge: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${groupId}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top',
    source: groupId,
    hidden: true,
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
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

  return { newGroupNode, newGroupEdge, rect }
}
