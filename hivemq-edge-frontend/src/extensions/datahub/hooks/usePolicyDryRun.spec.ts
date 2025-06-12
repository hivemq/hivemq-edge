import { beforeEach, expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import type { Node } from '@xyflow/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { onlyNonNullResources, usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import type { BehaviorPolicyData, DataPolicyData, DryRunResults } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType } from '@datahub/types.ts'

describe('onlyNonNullResources', () => {
  it('should return an async function', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
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
    const result: DryRunResults<Node<DataPolicyData>, never> = {
      node: MOCK_NODE_DATA_POLICY,
      resources: [{ node: MOCK_NODE_TEST }],
    }

    expect(onlyNonNullResources([], {} as DryRunResults<unknown, never>)).toStrictEqual([])
    expect(onlyNonNullResources([], result)).toStrictEqual([{ node: MOCK_NODE_TEST }])
  })
})

describe('usePolicyDryRun', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  it('should validate a Data Policy', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun, { wrapper })
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
      data: { id: 'my-policy-id', model: BehaviorPolicyType.PUBLISH_DUPLICATE },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun, { wrapper })
    await act(async () => {
      const results = await result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)
      expect(results).toHaveLength(3)
      const { node } = results[0]
      expect(node).toStrictEqual(MOCK_NODE_DATA_POLICY)
    })
  })

  it('should return an error otherwise', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: 'test',
      data: { id: 'node-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { result } = renderHook(usePolicyDryRun, { wrapper })
    await expect(result.current.checkPolicyAsync(MOCK_NODE_DATA_POLICY)).rejects.toEqual(
      Error('Policy Type not supported : test')
    )
  })
})
