import { Edge, MarkerType } from 'reactflow'

// TODO[NVL] Too restrictive for theming; use custom edge
export const styleDefaultEdge: Pick<Edge, 'markerEnd' | 'markerStart' | 'style'> = {
  markerEnd: {
    type: MarkerType.ArrowClosed,
    width: 20,
    height: 20,
    color: '#008c2d',
  },
  style: {
    strokeWidth: 2,
    stroke: 'var(--chakra-colors-green-500)',
  },
}
