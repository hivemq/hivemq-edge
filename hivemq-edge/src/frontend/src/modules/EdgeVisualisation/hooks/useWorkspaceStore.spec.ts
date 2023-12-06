import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { NodeAddChange, EdgeAddChange, Node, Edge } from 'reactflow'

import { WorkspaceAction, WorkspaceState } from '../types.ts'
import useWorkspaceStore from './useWorkspaceStore.ts'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'

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
})
