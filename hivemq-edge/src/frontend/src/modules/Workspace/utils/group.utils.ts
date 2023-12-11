import { getRectOfNodes, Node, Rect } from 'reactflow'

const GROUP_MARGIN = 20
const GROUP_TITLE_MARGIN = 24

export const getGroupLayout = (nodes: Node[]): Rect => {
  const rect = getRectOfNodes(nodes.filter((e) => e !== undefined))

  return {
    x: rect.x - GROUP_MARGIN,
    y: rect.y - GROUP_MARGIN - GROUP_TITLE_MARGIN,
    width: rect.width + 2 * GROUP_MARGIN,
    height: rect.height + 2 * GROUP_MARGIN + GROUP_TITLE_MARGIN,
  }
}
