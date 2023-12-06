import { create } from 'zustand'
import {
  EdgeChange,
  NodeChange,
  NodeAddChange,
  EdgeAddChange,
  Node,
  addEdge,
  applyNodeChanges,
  applyEdgeChanges,
  Edge,
} from 'reactflow'
import { Group, NodeTypes, WorkspaceState, WorkspaceAction } from '@/modules/EdgeVisualisation/types.ts'
import { persist, createJSONStorage } from 'zustand/middleware'
import { Adapter } from '@/api/__generated__'

// define the initial state
const initialState: WorkspaceState = {
  nodes: [],
  edges: [],
}

const useWorkspaceStore = create<WorkspaceState & WorkspaceAction>()(
  persist(
    (set, get) => ({
      ...initialState,
      reset: () => {
        set(initialState)
      },
      onNodesChange: (changes: NodeChange[]) => {
        set({
          nodes: applyNodeChanges(changes, get().nodes),
        })
      },
      onEdgesChange: (changes: EdgeChange[]) => {
        set({
          edges: applyEdgeChanges(changes, get().edges),
        })
      },
      onInsertGroupNode: (parentNode: Node<Group, NodeTypes.CLUSTER_NODE>, edge: Edge) => {
        const nodeIds = parentNode.data.childrenNodeIds
        let pos = 0
        set({
          nodes: [
            parentNode,
            ...get().nodes.map((node) => {
              if (nodeIds.includes(node.id)) {
                pos += 25
                return {
                  ...node,
                  position: { x: pos, y: pos },
                  parentNode: parentNode.id,
                  expandParent: true,
                  selected: false,
                }
              }
              return node
            }),
          ],
        })

        set({
          edges: addEdge(edge, get().edges),
        })
      },
      updateNodeParent: (nodeIds: string[], parentNode: string | undefined) => {
        set({
          nodes: get().nodes.map((node) => {
            let pos = 0
            if (nodeIds.includes(node.id)) {
              pos += 10
              // it's important to create a new object here, to inform React Flow about the changes
              return {
                ...node,
                position: { x: pos, y: pos },
                parentNode,
                expandParent: true,
              }
            }
            return node
          }),
        })
      },
      onAddNodes: (changes: NodeAddChange[]) => {
        const nodeIDs = get().nodes.map((e) => e.id)

        const addChanges: NodeChange[] = changes.filter((e) => !nodeIDs.includes(e.item.id))
        if (addChanges.length)
          set({
            nodes: applyNodeChanges(addChanges, get().nodes),
          })
      },
      onAddEdges: (changes: EdgeAddChange[]) => {
        const ndID = get().edges.map((e) => e.id)

        const addChanges: EdgeChange[] = changes.filter((e) => !ndID.includes(e.item.id))
        if (addChanges.length)
          set({
            edges: applyEdgeChanges(addChanges, get().edges),
          })
      },
      onDeleteNode: (type: NodeTypes, adapterId: string) => {
        set({
          nodes: get().nodes.filter((e) => {
            return !(e.type === type && (e.data as Adapter).id === adapterId)
          }),
        })
      },
      onToggleGroup: (node: Pick<Node<Group, NodeTypes.CLUSTER_NODE>, 'id' | 'data'>, showGroup: boolean) => {
        set({
          edges: get().edges.map((edge) => {
            if (edge.source === node.id) return { ...edge, hidden: showGroup }
            if (node.data?.childrenNodeIds.includes(edge.source)) return { ...edge, hidden: !showGroup }
            return edge
          }),
        })
      },
    }),
    {
      name: 'edge.workspace',
      storage: createJSONStorage(() => localStorage),
    }
  )
)

export default useWorkspaceStore
