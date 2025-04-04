import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'

import type { DataPolicyData, DryRunResults, PolicyCheckAction, PolicyCheckState } from '../types.ts'
import { DataHubNodeType, PolicyDryRunStatus } from '../types.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import type { Node } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.DATA_POLICY,
  data: { id: 'my-policy-id' },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

describe('usePolicyChecksStore', () => {
  beforeEach(() => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)
    act(() => {
      result.current.reset()
    })
  })

  it('should start with an empty store', async () => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)
    const { node, report, status } = result.current

    expect(node).toBeUndefined()
    expect(report).toBeUndefined()
    expect(status).toEqual(PolicyDryRunStatus.IDLE)
  })

  it('should set the node', async () => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)
    expect(result.current.node).toBeUndefined()

    act(() => {
      const { setNode } = result.current
      setNode(MOCK_NODE_DATA_POLICY)
    })

    expect(result.current.node).toEqual(
      expect.objectContaining({
        id: 'node-id',
      })
    )
  })

  it('should set the report', async () => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)
    expect(result.current.report).toBeUndefined()
    expect(result.current.status).toEqual(PolicyDryRunStatus.IDLE)

    act(() => {
      const { setReport } = result.current
      const dd: DryRunResults<Node<DataPolicyData>, never> = {
        node: MOCK_NODE_DATA_POLICY,
      }
      setReport([dd])
    })
    expect(result.current.status).toEqual(PolicyDryRunStatus.SUCCESS)
    expect(result.current.report).toHaveLength(1)
    expect(result.current.report?.[0]).toEqual(
      expect.objectContaining({
        node: MOCK_NODE_DATA_POLICY,
      })
    )

    act(() => {
      const { setReport } = result.current
      const dd: DryRunResults<Node<DataPolicyData>, never> = {
        node: MOCK_NODE_DATA_POLICY,
        error: { title: 'error title', status: 404, type: 'error-type' },
      }
      setReport([dd])
    })
    expect(result.current.status).toEqual(PolicyDryRunStatus.FAILURE)
    expect(result.current.report).toHaveLength(1)
    expect(result.current.report?.[0]).toEqual(
      expect.objectContaining({
        node: MOCK_NODE_DATA_POLICY,
      })
    )
  })

  it('should get the errors', async () => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)

    act(() => {
      const { getErrors } = result.current
      const errors = getErrors()
      expect(errors).toBeUndefined()
    })

    act(() => {
      const { setReport } = result.current
      const dd: DryRunResults<Node<DataPolicyData>, never> = {
        node: MOCK_NODE_DATA_POLICY,
      }
      setReport([dd])
    })

    expect(result.current.report).toHaveLength(1)
    act(() => {
      const { getErrors } = result.current

      const errors = getErrors()
      expect(errors).toHaveLength(0)
    })

    act(() => {
      const { setReport } = result.current
      const dd: DryRunResults<Node<DataPolicyData>, never> = {
        node: MOCK_NODE_DATA_POLICY,
        error: { title: 'error title', status: 404, type: 'error-type' },
      }
      setReport([dd])
    })
    expect(result.current.report).toHaveLength(1)
    act(() => {
      const { getErrors } = result.current

      const errors = getErrors()
      expect(errors).toHaveLength(1)
      expect(errors?.[0]).toEqual(expect.objectContaining({ title: 'error title', status: 404, type: 'error-type' }))
    })
  })

  it('should initialise the report', async () => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)
    expect(result.current.report).toBeUndefined()
    expect(result.current.status).toEqual(PolicyDryRunStatus.IDLE)

    act(() => {
      const { setReport } = result.current
      const dd: DryRunResults<Node<DataPolicyData>, never> = {
        node: MOCK_NODE_DATA_POLICY,
      }
      setReport([dd])
    })
    expect(result.current.report).not.toBeUndefined()
    expect(result.current.status).toEqual(PolicyDryRunStatus.SUCCESS)

    act(() => {
      const { initReport } = result.current
      initReport()
    })
    expect(result.current.report).toBeUndefined()
    expect(result.current.status).toEqual(PolicyDryRunStatus.RUNNING)
  })

  it('should reset the store', async () => {
    const { result } = renderHook<PolicyCheckState & PolicyCheckAction, unknown>(usePolicyChecksStore)

    act(() => {
      const { setNode } = result.current
      setNode(MOCK_NODE_DATA_POLICY)
    })
    expect(result.current.node).not.toBeUndefined()

    act(() => {
      const { reset } = result.current
      reset()
    })
    expect(result.current.node).toBeUndefined()
  })
})
