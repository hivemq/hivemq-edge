import type { InternalNode, Node, XYPosition } from '@xyflow/react'
import { Position, MarkerType } from '@xyflow/react'
import { CONFIG_ADAPTER_WIDTH } from './nodes-utils'

// this helper function returns the intersection point
// of the line between the center of the intersectionNode and the target node
// https://math.stackexchange.com/questions/1724792/an-algorithm-for-finding-the-intersection-point-between-a-center-of-vision-and-a
/**
 * this helper function returns the intersection point of the line between the center of the intersectionNode and the target node
 * @see https://math.stackexchange.com/questions/1724792/an-algorithm-for-finding-the-intersection-point-between-a-center-of-vision-and-a
 * @param intersectionNode
 * @param targetNode
 */
export function getNodeIntersection(intersectionNode: InternalNode<Node>, targetNode: InternalNode<Node>) {
  const { width: intersectionNodeWidth, height: intersectionNodeHeight } = intersectionNode.measured
  const intersectionNodePosition = intersectionNode.internals.positionAbsolute
  const targetPosition = targetNode.internals.positionAbsolute

  const w = (intersectionNodeWidth || 0) / 2
  const h = (intersectionNodeHeight || 0) / 2

  const x2 = intersectionNodePosition.x + w
  const y2 = intersectionNodePosition.y + h
  const x1 = targetPosition.x + (targetNode.measured.width || 0) / 2
  const y1 = targetPosition.y + (targetNode.measured.height || 0) / 2

  const xx1 = (x1 - x2) / (2 * w) - (y1 - y2) / (2 * h)
  const yy1 = (x1 - x2) / (2 * w) + (y1 - y2) / (2 * h)
  const a = 1 / (Math.abs(xx1) + Math.abs(yy1))
  const xx3 = a * xx1
  const yy3 = a * yy1
  const x = w * (xx3 + yy3) + x2
  const y = h * (-xx3 + yy3) + y2

  return { x, y } as XYPosition
}

// returns the position (top,right,bottom or right) passed node compared to the intersection point
export function getEdgePosition(node: InternalNode<Node>, intersectionPoint: XYPosition) {
  const n = { ...node.internals.positionAbsolute, ...node }
  const nx = Math.round(n.x)
  const ny = Math.round(n.y)
  const px = Math.round(intersectionPoint.x)
  const py = Math.round(intersectionPoint.y)

  if (px <= nx + 1) {
    return Position.Left
  }
  if (px >= nx + (n.measured.width || 0) - 1) {
    return Position.Right
  }
  if (py <= ny + 1) {
    return Position.Top
  }
  if (py >= n.y + (n.measured.height || 0) - 1) {
    return Position.Bottom
  }

  return Position.Top
}

// returns the parameters (sx, sy, tx, ty, sourcePos, targetPos) you need to create an edge
export function getEdgeParams(source: InternalNode<Node>, target: InternalNode<Node>) {
  const sourceIntersectionPoint = getNodeIntersection(source, target)
  const targetIntersectionPoint = getNodeIntersection(target, source)

  const sourcePos = getEdgePosition(source, sourceIntersectionPoint)
  const targetPos = getEdgePosition(target, targetIntersectionPoint)

  return {
    sx: sourceIntersectionPoint.x,
    sy: sourceIntersectionPoint.y,
    tx: targetIntersectionPoint.x,
    ty: targetIntersectionPoint.y,
    sourcePos,
    targetPos,
  }
}

/* istanbul ignore next -- @preserve */
export function initialElements() {
  const nodes = []
  const edges = []
  const center = { x: window.innerWidth / 2, y: window.innerHeight / 2 }

  nodes.push({ id: 'target', data: { label: 'Target' }, position: center })

  const OCTAGON_DISTRIBUTION = 8

  for (let i = 0; i < OCTAGON_DISTRIBUTION; i++) {
    const degrees = i * (360 / OCTAGON_DISTRIBUTION)
    const radians = degrees * (Math.PI / 180)
    const x = CONFIG_ADAPTER_WIDTH * Math.cos(radians) + center.x
    const y = CONFIG_ADAPTER_WIDTH * Math.sin(radians) + center.y

    nodes.push({ id: `${i}`, data: { label: 'Source' }, position: { x, y } })

    edges.push({
      id: `edge-${i}`,
      target: 'target',
      source: `${i}`,
      type: 'floating',
      markerEnd: {
        type: MarkerType.Arrow,
      },
    })
  }

  return { nodes, edges }
}
