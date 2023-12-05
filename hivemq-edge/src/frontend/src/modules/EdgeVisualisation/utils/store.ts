import { create } from 'zustand'
import { Connection, EdgeChange, NodeChange, addEdge, applyNodeChanges, applyEdgeChanges } from 'reactflow'
import { RFState } from '@/modules/EdgeVisualisation/types.ts'
import { persist, createJSONStorage } from 'zustand/middleware'

const useWorkspaceStore = create<RFState>()(
  persist(
    (set, get) => ({
      nodes: [],
      edges: [],
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
          edges: addEdge(connection, get().edges),
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
