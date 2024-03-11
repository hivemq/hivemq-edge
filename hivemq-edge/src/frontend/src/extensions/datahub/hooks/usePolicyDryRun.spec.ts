import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { resourceReducer, usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import {
  BehaviorPolicyData,
  BehaviorPolicyType,
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
} from '@datahub/types.ts'
import { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

describe('resourceReducer', () => {
  it('should return an async function', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_NODE_TEST: Node<DataPolicyData> = {
      id: 'node-test',
      type: DataHubNodeType.DATA_POLICY,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const result: DryRunResults<Node<DataPolicyData>, never> = {
      node: MOCK_NODE_DATA_POLICY,
      resources: [{ node: MOCK_NODE_TEST }],
    }

    expect(resourceReducer([], {} as DryRunResults<unknown, never>)).toStrictEqual([])
    expect(resourceReducer([], result)).toStrictEqual([{ node: MOCK_NODE_TEST }])
  })
})

describe('usePolicyDryRun', () => {
  it('should validate a Data Policy', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun)
    await act(async () => {
      const results = await result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)
      expect(results).toHaveLength(1)
      const { node } = results[0]
      expect(node).toStrictEqual(MOCK_NODE_DATA_POLICY)
    })
  })

  it('should validate a Behaviour Policy', async () => {
    const MOCK_NODE_DATA_POLICY: Node<BehaviorPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.BEHAVIOR_POLICY,
      data: { model: BehaviorPolicyType.PUBLISH_DUPLICATE },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun)
    await act(async () => {
      const results = await result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)
      expect(results).toHaveLength(4)
      const { node } = results[0]
      expect(node).toStrictEqual(MOCK_NODE_DATA_POLICY)
    })
  })

  it('should return an error otherwise', async () => {
    const MOCK_NODE_DATA_POLICY: Node = {
      id: 'node-id',
      type: 'test',
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun)
    await expect(result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)).rejects.toEqual(
      Error('Policy Type not supported : test')
    )
  })
})
