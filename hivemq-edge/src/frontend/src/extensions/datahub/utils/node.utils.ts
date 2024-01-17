import { Edge, Node } from 'reactflow'
import { CSSProperties } from 'react'

export const styleSourceHandle: CSSProperties = {
  width: '12px',
  right: '-6px',
  borderRadius: 0,
  height: '12px',
}

export const initialFlow = () => {
  const nodes: Node[] = []
  const edges: Edge[] = []

  return { nodes, edges }
}
