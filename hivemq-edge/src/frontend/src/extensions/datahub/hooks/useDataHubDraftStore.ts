import {
  Connection,
  EdgeAddChange,
  EdgeChange,
  NodeAddChange,
  NodeChange,
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  Node,
} from 'reactflow'
import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

import {
  BehaviorPolicyData,
  DataHubNodeType,
  DataPolicyData,
  DesignerStatus,
  EdgeTypes,
  FunctionSpecs,
  WorkspaceAction,
  WorkspaceState,
  WorkspaceStatus,
} from '../types.ts'
import { initialStore } from '../utils/store.utils.ts'
import { styleDefaultEdge } from '../utils/edge.utils.ts'

const useDataHubDraftStore = create<WorkspaceState & WorkspaceStatus & WorkspaceAction>()(
  persist(
    (set, get) => ({
      ...initialStore(),
      reset: () => {
        set(initialStore())
      },
      isDirty: () => {
        return get().nodes.length !== 0 //&& get().edges.length !== 0
      },
      isPolicyInDraft: () => {
        return (
          get().nodes.filter(
            (node) => node.type === DataHubNodeType.DATA_POLICY || node.type === DataHubNodeType.BEHAVIOR_POLICY
          ).length !== 0
        )
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
              type: EdgeTypes.DATAHUB_EDGE,
              markerEnd: styleDefaultEdge.markerEnd,
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
      onSerializePolicy: (node: Node<DataPolicyData | BehaviorPolicyData>): string | undefined => {
        if (node.type !== DataHubNodeType.BEHAVIOR_POLICY && node.type !== DataHubNodeType.DATA_POLICY) return undefined
        return undefined
      },
      setStatus: (
        status: DesignerStatus,
        option?: { name?: string; type?: DataHubNodeType.DATA_POLICY | DataHubNodeType.BEHAVIOR_POLICY }
      ) => {
        set({ status: status })
        if (option && option.name != undefined) set({ name: option.name })
        if (option?.type) set({ type: option.type })
      },
    }),
    {
      name: 'datahub.workspace',
      storage: createJSONStorage(() => localStorage),
    }
  )
)

export default useDataHubDraftStore
