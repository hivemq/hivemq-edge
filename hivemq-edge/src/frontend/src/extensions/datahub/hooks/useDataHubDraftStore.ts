import {
  Connection,
  EdgeAddChange,
  EdgeChange,
  NodeAddChange,
  NodeChange,
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
} from 'reactflow'
import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

import { FunctionSpecs, WorkspaceAction, WorkspaceState } from '../types.ts'
import { initialStore } from '../utils/store.utils.ts'
import { styleDefaultEdge } from '../utils/edge.utils.ts'

const useDataHubDraftStore = create<WorkspaceState & WorkspaceAction>()(
  persist(
    (set, get) => ({
      ...initialStore(),
      reset: () => {
        set(initialStore())
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
      onConnect: (connection: Connection) => {
        set({
          edges: addEdge(
            {
              ...connection,
              ...styleDefaultEdge,
            },
            get().edges
          ),
        })
      },
      onAddNodes: (changes: NodeAddChange[]) => {
        const nodeIDs = get().nodes.map((e) => e.id)

        const addChanges: NodeChange[] = changes.filter((e) => !nodeIDs.includes(e.item.id))
        if (addChanges.length) {
          set({
            nodes: applyNodeChanges(addChanges, get().nodes),
          })
        }
      },
      onAddEdges: (changes: EdgeAddChange[]) => {
        const ndID = get().edges.map((e) => e.id)

        const addChanges: EdgeChange[] = changes.filter((e) => !ndID.includes(e.item.id))
        if (addChanges.length) {
          set({
            edges: applyEdgeChanges(addChanges, get().edges),
          })
        }
      },
      onUpdateNodes: <T>(item: string, data: T) => {
        set({
          nodes: get().nodes.map((node) => {
            if (node.id === item) {
              node.data = data
            }
            return node
          }),
        })
      },

      onAddFunctions: (changes: FunctionSpecs[]) => {
        set({ functions: [...get().functions, ...changes] })
      },
    }),
    {
      name: 'datahub.workspace',
      storage: createJSONStorage(() => localStorage),
    }
  )
)

export default useDataHubDraftStore
