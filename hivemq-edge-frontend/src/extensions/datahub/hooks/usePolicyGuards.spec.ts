import { expect } from 'vitest'
import { renderHook } from '@testing-library/react'

import { getPolicyWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'
import { DesignerStatus } from '@datahub/types.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

describe('usePolicyGuards', () => {
  it('should render guards for DRAFT', async () => {
    const { result } = renderHook(() => usePolicyGuards())
    expect(result.current.status).toStrictEqual(DesignerStatus.DRAFT)
    expect(result.current.isPolicyEditable).toBeTruthy()
    expect(result.current.isNodeEditable).toBeFalsy()
    expect(result.current.guardAlert).toBeUndefined()
  })

  it('should render guards for a read-only policy', async () => {
    const { result } = renderHook(() => usePolicyGuards('my-topic'), {
      wrapper: getPolicyWrapper({ status: DesignerStatus.LOADED }),
    })
    expect(result.current.status).toStrictEqual(DesignerStatus.LOADED)
    expect(result.current.isPolicyEditable).toBeFalsy()
    expect(result.current.isNodeEditable).toBeFalsy()
    expect(result.current.guardAlert).toStrictEqual({
      title: 'The policy is in read-only mode',
      description: 'The element cannot be modified',
    })
  })

  it('should render guards for a modified policy', async () => {
    const { result } = renderHook(() => usePolicyGuards('my-topic'), {
      wrapper: getPolicyWrapper({ status: DesignerStatus.LOADED }),
    })
    expect(result.current.status).toStrictEqual(DesignerStatus.LOADED)
    expect(result.current.isPolicyEditable).toBeFalsy()
    expect(result.current.isNodeEditable).toBeFalsy()
    expect(result.current.guardAlert).toStrictEqual({
      title: 'The policy is in read-only mode',
      description: 'The element cannot be modified',
    })
  })

  it('should render guards for a protected node', async () => {
    const { result } = renderHook(() => usePolicyGuards('node-id'), {
      wrapper: getPolicyWrapper({ status: DesignerStatus.MODIFIED, nodes: [MOCK_NODE_DATA_POLICY] }),
    })
    expect(result.current.status).toStrictEqual(DesignerStatus.MODIFIED)
    expect(result.current.isPolicyEditable).toBeTruthy()
    expect(result.current.isNodeEditable).toBeFalsy()
    expect(result.current.guardAlert).toStrictEqual({
      title: 'The element is protected',
      description: 'The element cannot be modified',
    })
  })
})
