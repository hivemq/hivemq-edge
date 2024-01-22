import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { NodeAddChange, EdgeAddChange, Node, Edge, NodeProps } from 'reactflow'

import { DataHubNodeType, WorkspaceAction, WorkspaceState } from '../types.ts'
import useDataHubDraftStore from '@/extensions/datahub/hooks/useDataHubDraftStore.ts'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

const MOCK_NODE: NodeProps<{ label: string }> = {
  id: 'idAdapter',
  type: DataHubNodeType.TOPIC_FILTER,
  data: { label: 'Hello1' },
  ...MOCK_DEFAULT_NODE,
}

describe('useDataHubDraftStore', () => {
  beforeEach(() => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    act(() => {
      result.current.reset()
    })
  })

  it('should start with an empty store', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    const { nodes, edges } = result.current

    expect(nodes).toHaveLength(0)
    expect(edges).toHaveLength(0)
  })

  it('should change nodes', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onNodesChange } = result.current
      const item: Partial<Node> = { ...MOCK_NODE, position: { x: 0, y: 0 } }
      onNodesChange([{ item, type: 'add' } as NodeAddChange])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)
  })

  it('should change edges', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
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
