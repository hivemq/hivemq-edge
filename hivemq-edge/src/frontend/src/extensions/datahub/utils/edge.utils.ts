import type { Edge } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'

// TODO[NVL] Too restrictive for theming; use custom edge
export const styleDefaultEdge: Pick<Edge, 'markerEnd' | 'markerStart' | 'style'> = {
  markerEnd: {
    type: MarkerType.ArrowClosed,
    width: 15,
    height: 20,
    color: 'var(--chakra-colors-gray-500)',
  },
  style: {
    strokeWidth: 2,
    stroke: 'var(--chakra-colors-gray-500)',
  },
}
