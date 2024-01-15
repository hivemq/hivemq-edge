import { Edge, Node } from 'reactflow'

export const initialFlow = () => {
  const nodes: Node[] = []
  const edges: Edge[] = []

  return { nodes, edges }
}
