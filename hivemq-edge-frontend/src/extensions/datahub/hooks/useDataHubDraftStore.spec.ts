import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import type { EdgeAddChange, Node, Edge, NodeProps } from '@xyflow/react'

import type { DataPolicyData, FunctionSpecs, WorkspaceAction, WorkspaceState } from '../types.ts'
import { DataHubNodeType } from '../types.ts'
import useDataHubDraftStore from '@/extensions/datahub/hooks/useDataHubDraftStore.ts'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

const MOCK_NODE: NodeProps<Node<{ label: string }>> = {
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
      const item: Node = { ...MOCK_NODE, position: { x: 0, y: 0 } }
      onNodesChange([{ item, type: 'add' }])
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

  it('should check if a policy is already in the store', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    const { nodes, edges } = result.current

    expect(nodes).toHaveLength(0)
    expect(edges).toHaveLength(0)

    act(() => {
      const { onNodesChange } = result.current
      const item1: Node = { ...MOCK_NODE, id: '1', position: { x: 0, y: 0 } }
      onNodesChange([{ item: item1, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { isPolicyInDraft } = result.current
      expect(isPolicyInDraft()).toBeFalsy()
    })

    act(() => {
      const { onNodesChange } = result.current
      const item2: Node = { ...MOCK_NODE, type: DataHubNodeType.BEHAVIOR_POLICY, id: '2', position: { x: 0, y: 0 } }
      onNodesChange([{ item: item2, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(2)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { isPolicyInDraft } = result.current
      expect(isPolicyInDraft()).toBeTruthy()
    })

    act(() => {
      const { reset } = result.current
      reset()
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onNodesChange } = result.current
      const item3: Node = { ...MOCK_NODE, type: DataHubNodeType.DATA_POLICY, id: '3', position: { x: 0, y: 0 } }
      onNodesChange([{ item: item3, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { isPolicyInDraft } = result.current
      expect(isPolicyInDraft()).toBeTruthy()
    })
  })

  it('should update the connection between two nodes', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onNodesChange } = result.current
      const item1: Node = { ...MOCK_NODE, id: '1', position: { x: 0, y: 0 } }
      const item2: Node = { ...MOCK_NODE, id: '2', position: { x: 0, y: 0 } }
      onNodesChange([
        { item: item1, type: 'add' },
        { item: item2, type: 'add' },
      ])
    })

    expect(result.current.nodes).toHaveLength(2)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onConnect } = result.current
      onConnect({ source: '1', target: '2', sourceHandle: null, targetHandle: null })
    })

    expect(result.current.nodes).toHaveLength(2)
    expect(result.current.edges).toHaveLength(1)
  })

  it("should add a node only if it doesn't exist", async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddNodes } = result.current
      const item1: Node = { ...MOCK_NODE, position: { x: 0, y: 0 } }
      onAddNodes([{ item: item1, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddNodes } = result.current
      const item1: Node = { ...MOCK_NODE, position: { x: 0, y: 0 } }
      onAddNodes([{ item: item1, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddNodes } = result.current
      const item1: Node = { ...MOCK_NODE, id: '2', position: { x: 0, y: 0 } }
      onAddNodes([{ item: item1, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(2)
    expect(result.current.edges).toHaveLength(0)
  })

  it("should add an edge only if it doesn't exist", async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddEdges } = result.current
      const item: Partial<Edge> = { id: '1-2', source: '1', target: '2' }
      onAddEdges([{ item, type: 'add' } as EdgeAddChange])
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(1)

    act(() => {
      const { onAddEdges } = result.current
      const item: Partial<Edge> = { id: '1-2', source: '1', target: '2' }
      onAddEdges([{ item, type: 'add' } as EdgeAddChange])
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(1)

    act(() => {
      const { onAddEdges } = result.current
      const item: Partial<Edge> = { id: '1-3', source: '1', target: '3' }
      onAddEdges([{ item, type: 'add' } as EdgeAddChange])
    })

    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(2)
  })

  it('should update the data of a node', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    expect(result.current.nodes).toHaveLength(0)
    expect(result.current.edges).toHaveLength(0)

    act(() => {
      const { onAddNodes } = result.current
      const item1: Node = { ...MOCK_NODE, position: { x: 0, y: 0 } }
      onAddNodes([{ item: item1, type: 'add' }])
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)
    expect(result.current.nodes[0].data).toEqual({ label: 'Hello1' })

    act(() => {
      const { onUpdateNodes } = result.current
      onUpdateNodes<{ label: string }>('idAdapter', { label: 'the new one' })
    })

    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.edges).toHaveLength(0)
    expect(result.current.nodes[0].data).toEqual({ label: 'the new one' })

    act(() => {
      const { onUpdateNodes } = result.current
      onUpdateNodes<{ label: string }>('fake', { label: 'a fake data' })
    })
    expect(result.current.nodes[0].data).toEqual({ label: 'the new one' })
  })

  it('should add a function to the store', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)
    expect(result.current.functions).toHaveLength(9)

    act(() => {
      const { onAddFunctions } = result.current
      const item: FunctionSpecs = { functionId: 'string' }
      onAddFunctions([item])
    })

    expect(result.current.functions).toHaveLength(10)
  })

  it('should serialise a policy', async () => {
    const { result } = renderHook<WorkspaceState & WorkspaceAction, unknown>(useDataHubDraftStore)

    act(() => {
      const { onSerializePolicy } = result.current
      const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
        id: 'node-id',
        type: DataHubNodeType.SCHEMA,
        data: { id: 'my-policy-id' },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }
      const res = onSerializePolicy(MOCK_NODE_DATA_POLICY)
      expect(res).toBeUndefined()
    })

    act(() => {
      const { onSerializePolicy } = result.current
      const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
        id: 'node-id',
        type: DataHubNodeType.DATA_POLICY,
        data: { id: 'my-other-policy-id' },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }
      const res = onSerializePolicy(MOCK_NODE_DATA_POLICY)
      expect(res).toBeUndefined()
    })
  })
})
