import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { EdgeAddChange, Node, Edge, Rect } from 'reactflow'

import { Group, IdStubs, NodeTypes, WorkspaceAction, WorkspaceState } from '../types.ts'
import useWorkspaceStore from './useWorkspaceStore.ts'
import {
  MOCK_NODE_ADAPTER,
  MOCK_NODE_BRIDGE,
  MOCK_NODE_EDGE,
  MOCK_NODE_GROUP,
} from '@/__test-utils__/react-flow/nodes.ts'

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
      const item: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      onNodesChange([{ item, type: 'add' }])
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
      const nodeEdge: Node = { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 0, y: 0 } }
      const item1: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      const item2: Node = { ...MOCK_NODE_BRIDGE, position: { x: 0, y: 0 } }

      const item: Edge = { id: '1-2', source: 'idAdapter', target: IdStubs.EDGE_NODE }

      onNodesChange([
        { item: nodeEdge, type: 'add' },
        { item: item1, type: 'add' },
        { item: item2, type: 'add' },
      ])
      onEdgesChange([{ item, type: 'add' }])
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

  it('should add a node', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddNodes } = result.current
      const item: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      onAddNodes([{ item, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)
  })

  it('should add an edge', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddEdges } = result.current
      const item: Partial<Edge> = { id: '1-2', source: '1', target: '2' }
      onAddEdges([{ item, type: 'add' } as EdgeAddChange])
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(1)
  })

  it('should delete a node', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onNodesChange } = result.current
      const item: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      onNodesChange([{ item, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onDeleteNode } = result.current
      onDeleteNode(NodeTypes.ADAPTER_NODE, MOCK_NODE_ADAPTER.data.id)
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)
  })

  it.skip('should toggle a group', async () => {
    // TODO
  })

  it('should change the data of a group', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useWorkspaceStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddNodes } = result.current
      const item: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
      const group: Node<Group> = { ...MOCK_NODE_GROUP, position: { x: 0, y: 0 } }
      onAddNodes([
        { item, type: 'add' },
        { item: group, type: 'add' },
      ])
    })
    expect(result.current.nodes).toHaveLength(2)
    expect(result.current.edges).toHaveLength(0)
    expect(result.current.nodes[1]).toStrictEqual(
      expect.objectContaining({
        id: 'idGroup',
        type: 'CLUSTER_NODE',
        data: expect.objectContaining({
          title: 'The group title',
        }),
      })
    )

    act(() => {
      const { onGroupSetData } = result.current
      onGroupSetData('idGroup', { title: 'a new title', colorScheme: 'green' })
    })

    expect(result.current.nodes).toHaveLength(2)
    expect(result.current.edges).toHaveLength(0)
    expect(result.current.nodes[1]).toStrictEqual(
      expect.objectContaining({
        id: 'idGroup',
        type: 'CLUSTER_NODE',
        data: expect.objectContaining({
          title: 'a new title',
          colorScheme: 'green',
        }),
      })
    )
  })
})
