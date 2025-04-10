import type { Node, Rect } from '@xyflow/react'
import { getNodesBounds } from '@xyflow/react'

const GROUP_MARGIN = 20
const GROUP_TITLE_MARGIN = 24

export const getGroupLayout = (nodes: Node[]): Rect => {
  const rect = getNodesBounds(nodes.filter((e) => e !== undefined))

  return {
    x: rect.x - GROUP_MARGIN,
    y: rect.y - GROUP_MARGIN - GROUP_TITLE_MARGIN,
    width: rect.width + 2 * GROUP_MARGIN,
    height: rect.height + 2 * GROUP_MARGIN + GROUP_TITLE_MARGIN,
  }
}
