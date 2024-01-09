import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { NodeAddChange, EdgeAddChange, Node, Edge, Rect } from 'reactflow'

import { Group, IdStubs, NodeTypes, WorkspaceAction, WorkspaceState } from '../types.ts'
import useWorkspaceStore from './useWorkspaceStore.ts'
import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE, MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes.ts'

describe('useWorkspaceStore', () => {
  beforeEach(() => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    act(() => {
      result.current.reset()
    })
  })

  it('should start with an empty store', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    const { nodes, edges } = result.current

    expect(nodes).toHaveLength(0)
    expect(edges).toHaveLength(0)
  })

  it('should change nodes', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onNodesChange } = result.current
      const item: Partial<Node> = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      onNodesChange([{ item, type: 'add' } as NodeAddChange])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)
  })

  it('should change edges', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onEdgesChange } = result.current
      const item: Partial<Edge> = { id: '1-2', source: '1', target: '2' }
      onEdgesChange([{ item, type: 'add' } as EdgeAddChange])
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(1)
  })

  it('should add a group in the store', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onNodesChange, onEdgesChange } = result.current
      const nodeEdge: Partial<Node> = { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 0, y: 0 } }
      const item1: Partial<Node> = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      const item2: Partial<Node> = { ...MOCK_NODE_BRIDGE, position: { x: 0, y: 0 } }

      const item: Partial<Edge> = { id: '1-2', source: 'idAdapter', target: IdStubs.EDGE_NODE }

      onNodesChange([
        { item: nodeEdge, type: 'add' } as NodeAddChange,
        { item: item1, type: 'add' } as NodeAddChange,
        { item: item2, type: 'add' } as NodeAddChange,
      ])
      onEdgesChange([{ item, type: 'add' } as EdgeAddChange])
    })
    expect(result.current.nodes).toHaveLength(3)
    expect(result.current.edges).toHaveLength(1)

    act(() => {
      const { onInsertGroupNode } = result.current
      const group: Node<Group, NodeTypes.CLUSTER_NODE> = {
        id: 'group1',
        position: { x: 0, y: 0 },
        data: { childrenNodeIds: ['idAdapter', 'idBridge'], title: 'my title', isOpen: true },
      }
      const rect: Rect = { x: 0, y: 0, width: 250, height: 250 }

      const groupEdge: Edge = { id: '1-233', source: '1', target: IdStubs.EDGE_NODE }

      onInsertGroupNode(group, groupEdge, rect)
    })

    expect(result.current.nodes).toHaveLength(4)
    expect(result.current.edges).toHaveLength(2)
    expect(result.current.nodes.find((e) => e.id === 'idAdapter')).toStrictEqual(
      expect.objectContaining({ parentNode: 'group1', selected: false, expandParent: true })
    )
    expect(result.current.nodes.find((e) => e.id === 'idBridge')).toStrictEqual(
      expect.objectContaining({ parentNode: 'group1', selected: false, expandParent: true })
    )
  })
})
