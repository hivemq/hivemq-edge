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
} from 'reactflow'
import { Group, NodeTypes, WorkspaceState, WorkspaceAction } from '@/modules/Workspace/types.ts'
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
      onInsertGroupNode: (parentNode, edge, rect) => {
        const nodeIds = parentNode.data.childrenNodeIds
        // let pos = 0
        set({
          nodes: [
            parentNode,
            ...get().nodes.map((node) => {
              if (nodeIds.includes(node.id)) {
                return {
                  ...node,
                  position: { x: node.position.x - rect.x, y: node.position.y - rect.y },
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
      onGroupSetData: (id: string, group: Pick<Group, 'title' | 'colorScheme'>) => {
        set({
          nodes: get().nodes.map((n) => {
            if (id === n.id) {
              return {
                ...n,
                data: {
                  ...n.data,
                  ...group,
                },
              }
            }
            return n
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
