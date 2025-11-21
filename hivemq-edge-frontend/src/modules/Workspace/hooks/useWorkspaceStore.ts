import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { EdgeChange, NodeChange, NodeAddChange, EdgeAddChange, Node } from '@xyflow/react'
import { addEdge, applyNodeChanges, applyEdgeChanges } from '@xyflow/react'

import type { Adapter } from '@/api/__generated__'
import type { Group, WorkspaceState, WorkspaceAction, DeviceMetadata } from '@/modules/Workspace/types.ts'
import { STORE_WORKSPACE_KEY } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { LayoutType, LayoutMode, type LayoutPreset, type LayoutHistoryEntry, type LayoutOptions } from '../types/layout'

// define the initial state
const initialState: WorkspaceState = {
  nodes: [],
  edges: [],
  // Layout configuration
  layoutConfig: {
    currentAlgorithm: LayoutType.MANUAL,
    mode: LayoutMode.STATIC,
    options: {
      animate: true,
      animationDuration: 300,
      fitView: true,
    },
    presets: [],
  },
  isAutoLayoutEnabled: false,
  layoutHistory: [],
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
                  parentId: parentNode.id,
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
          nodes: get().nodes.filter((node) => {
            const isThisTheAdapter = node.type === type && (node.data as Adapter).id === adapterId
            const isThisTheDevice =
              node.type === NodeTypes.DEVICE_NODE && (node.data as DeviceMetadata).sourceAdapterId == adapterId

            return !isThisTheAdapter && !isThisTheDevice
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
      onUpdateNode: <T extends Record<string, unknown>>(id: string, data: T) => {
        set({
          nodes: get().nodes.map((node) => {
            if (id === node.id) {
              return {
                ...node,
                data: data,
              }
            }
            return node
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
      // Layout actions
      setLayoutAlgorithm: (algorithm: LayoutType) => {
        set((state) => ({
          layoutConfig: {
            ...state.layoutConfig,
            currentAlgorithm: algorithm,
          },
        }))
      },
      setLayoutMode: (mode: LayoutMode) => {
        set((state) => ({
          layoutConfig: {
            ...state.layoutConfig,
            mode,
          },
        }))
      },
      setLayoutOptions: (options: Partial<LayoutOptions>) => {
        set((state) => ({
          layoutConfig: {
            ...state.layoutConfig,
            options: {
              ...state.layoutConfig.options,
              ...options,
            },
          },
        }))
      },
      toggleAutoLayout: () => {
        set((state) => ({
          isAutoLayoutEnabled: !state.isAutoLayoutEnabled,
        }))
      },
      saveLayoutPreset: (preset: LayoutPreset) => {
        set((state) => ({
          layoutConfig: {
            ...state.layoutConfig,
            presets: [...state.layoutConfig.presets, preset],
          },
        }))
      },
      loadLayoutPreset: (presetId: string) => {
        const state = get()
        const preset = state.layoutConfig.presets.find((p) => p.id === presetId)
        if (preset) {
          set({
            layoutConfig: {
              ...state.layoutConfig,
              currentAlgorithm: preset.algorithm,
              options: preset.options,
            },
          })
        }
      },
      deleteLayoutPreset: (presetId: string) => {
        set((state) => ({
          layoutConfig: {
            ...state.layoutConfig,
            presets: state.layoutConfig.presets.filter((p) => p.id !== presetId),
          },
        }))
      },
      pushLayoutHistory: (entry: LayoutHistoryEntry) => {
        set((state) => {
          const newHistory = [...state.layoutHistory, entry]
          // Keep only last 20 entries
          if (newHistory.length > 20) {
            newHistory.shift()
          }
          return { layoutHistory: newHistory }
        })
      },
      clearLayoutHistory: () => {
        set({ layoutHistory: [] })
      },
    }),
    {
      name: STORE_WORKSPACE_KEY,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => {
        // Always persist nodes and edges (core workspace state)
        const persisted: Partial<WorkspaceState> = {
          nodes: state.nodes,
          edges: state.edges,
        }

        persisted.layoutConfig = state.layoutConfig
        persisted.isAutoLayoutEnabled = state.isAutoLayoutEnabled
        // Don't persist layoutHistory - it's ephemeral

        return persisted
      },
    }
  )
)

export default useWorkspaceStore
