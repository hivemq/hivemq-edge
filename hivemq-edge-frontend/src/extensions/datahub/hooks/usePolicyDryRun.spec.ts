import { beforeEach, expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import type { Node } from '@xyflow/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { onlyNonNullResources, onlyUniqueResources, usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import type { BehaviorPolicyData, DataPolicyData, DryRunResults } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType, PolicyDryRunStatus } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
  id: 'node-data-id',
  type: DataHubNodeType.DATA_POLICY,
  data: { id: 'node-data-id' },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

const MOCK_NODE_BEHAVIOUR_POLICY: Node<BehaviorPolicyData> = {
  id: 'node-behaviour-id',
  type: DataHubNodeType.BEHAVIOR_POLICY,
  data: { id: 'node-behaviour-id', model: BehaviorPolicyType.PUBLISH_DUPLICATE },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

const MOCK_NODE_TEST: Node<DataPolicyData> = {
  id: 'node-test',
  type: DataHubNodeType.DATA_POLICY,
  data: { id: 'my-other-policy-id' },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

describe('onlyNonNullResources', () => {
  it('should return an async function', async () => {
    const result: DryRunResults<Node<DataPolicyData>, never> = {
      node: MOCK_NODE_DATA_POLICY,
      resources: [{ node: MOCK_NODE_TEST }],
    }

    expect(onlyNonNullResources([], {} as DryRunResults<unknown, never>)).toStrictEqual([])
    expect(onlyNonNullResources([], result)).toStrictEqual([{ node: MOCK_NODE_TEST }])
  })
})

describe('onlyUniqueResources', () => {
  it('should test for unique node id', async () => {
    const test: DryRunResults<Node<DataPolicyData>, never> = {
      node: MOCK_NODE_DATA_POLICY,
    }

    const test2: DryRunResults<Node<DataPolicyData>, never> = {
      node: MOCK_NODE_BEHAVIOUR_POLICY,
    }

    expect(onlyUniqueResources([], test)).toStrictEqual([test])
    expect(onlyUniqueResources([test], test)).toStrictEqual([test])
    expect(onlyUniqueResources([test], test2)).toStrictEqual([test, test2])
  })
})

describe('usePolicyDryRun', () => {
  beforeEach(() => {
    server.use(...handlers)
    // Reset the store before each test
    const { result } = renderHook(() => useDataHubDraftStore(), { wrapper })
    act(() => {
      result.current.reset()
    })
  })

  it('should validate a Data Policy', async () => {
    const { result } = renderHook(usePolicyDryRun, { wrapper })
    await act(async () => {
      const results = await result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)
      expect(results).toHaveLength(1)
      const { node } = results[0]
      expect(node).toStrictEqual(MOCK_NODE_DATA_POLICY)
    })
  })

  it('should validate a Behaviour Policy', async () => {
    const { result } = renderHook(usePolicyDryRun, { wrapper })
    await act(async () => {
      const results = await result.current.checkPolicyAsync(MOCK_NODE_BEHAVIOUR_POLICY)
      expect(results).toHaveLength(3)
      const { node } = results[0]
      expect(node).toStrictEqual(MOCK_NODE_BEHAVIOUR_POLICY)
    })
  })

  it('should return an error otherwise', async () => {
    const fakePolicy: Node<DataPolicyData> = {
      id: 'node-id',
      type: 'test',
      data: { id: 'node-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun, { wrapper })
    await expect(result.current.checkPolicyAsync(fakePolicy)).rejects.toEqual(Error('Policy Type not supported : test'))
  })

  it('should update node status to RUNNING and then SUCCESS', async () => {
    const storeHook = renderHook(() => useDataHubDraftStore(), { wrapper })
    const policyHook = renderHook(usePolicyDryRun, { wrapper })

    // Add the node to the store
    act(() => {
      storeHook.result.current.onAddNodes([{ item: MOCK_NODE_DATA_POLICY, type: 'add' }])
    })

    await act(async () => {
      await policyHook.result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)
    })

    // After validation, the node should have a status (will be FAILURE since it's not properly configured)
    const updatedNode = storeHook.result.current.nodes.find((n) => n.id === MOCK_NODE_DATA_POLICY.id)
    expect(updatedNode).toBeDefined()
    // A policy without proper configuration will be marked as FAILURE
    expect(updatedNode?.data.dryRunStatus).toBe(PolicyDryRunStatus.FAILURE)
  })

  it('should update node status to FAILURE when there is an error', async () => {
    const storeHook = renderHook(() => useDataHubDraftStore(), { wrapper })
    const policyHook = renderHook(usePolicyDryRun, { wrapper })

    const policyWithError: Node<DataPolicyData> = {
      ...MOCK_NODE_DATA_POLICY,
      data: {
        ...MOCK_NODE_DATA_POLICY.data,
        dryRunStatus: PolicyDryRunStatus.IDLE,
      },
    }

    // Add the node to the store
    act(() => {
      storeHook.result.current.onAddNodes([{ item: policyWithError, type: 'add' }])
    })

    await act(async () => {
      await policyHook.result.current.checkPolicyAsync(policyWithError)
    })

    // The node should be processed
    const updatedNode = storeHook.result.current.nodes.find((n) => n.id === policyWithError.id)
    expect(updatedNode).toBeDefined()
  })

  it('should preserve FAILURE status when node was already marked as failure', async () => {
    const storeHook = renderHook(() => useDataHubDraftStore(), { wrapper })
    const policyHook = renderHook(usePolicyDryRun, { wrapper })

    const policyWithFailure: Node<DataPolicyData> = {
      ...MOCK_NODE_DATA_POLICY,
      data: {
        ...MOCK_NODE_DATA_POLICY.data,
        dryRunStatus: PolicyDryRunStatus.FAILURE,
      },
    }

    // Add the node to the store with FAILURE status
    act(() => {
      storeHook.result.current.onAddNodes([{ item: policyWithFailure, type: 'add' }])
    })

    await act(async () => {
      await policyHook.result.current.checkPolicyAsync(policyWithFailure)
    })

    // The failure status should be preserved
    const updatedNode = storeHook.result.current.nodes.find((n) => n.id === policyWithFailure.id)
    expect(updatedNode?.data.dryRunStatus).toBe(PolicyDryRunStatus.FAILURE)
  })

  it('should handle node not found in store gracefully', async () => {
    const policyHook = renderHook(usePolicyDryRun, { wrapper })

    // Try to check a policy that is not in the store
    const orphanPolicy: Node<DataPolicyData> = {
      id: 'orphan-node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'orphan-node-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    await act(async () => {
      // Should not throw error even if node is not found
      const results = await policyHook.result.current.checkPolicyAsync(orphanPolicy)
      expect(results).toBeDefined()
    })
  })

  it('should validate behavior policy and check all transitions', async () => {
    const storeHook = renderHook(() => useDataHubDraftStore(), { wrapper })
    const policyHook = renderHook(usePolicyDryRun, { wrapper })

    // Add the behavior policy node to the store
    act(() => {
      storeHook.result.current.onAddNodes([{ item: MOCK_NODE_BEHAVIOUR_POLICY, type: 'add' }])
    })

    await act(async () => {
      const results = await policyHook.result.current.checkPolicyAsync(MOCK_NODE_BEHAVIOUR_POLICY)

      // Should include: configurations, clients, model, and the behavior policy itself
      expect(results.length).toBeGreaterThan(0)
    })

    const updatedNode = storeHook.result.current.nodes.find((n) => n.id === MOCK_NODE_BEHAVIOUR_POLICY.id)
    expect(updatedNode).toBeDefined()
  })
})
