import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { InterpolationVariable, InterpolationVariableList } from '@/api/__generated__'
import { PolicyType } from '@/api/__generated__'
import { useGetInterpolationVariables } from '@datahub/api/hooks/DataHubInterpolationService/useGetInterpolationVariables.ts'

import { handlers } from './__handlers__'

describe('useGetInterpolationVariables', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetInterpolationVariables, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<InterpolationVariableList>({
      items: [
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'clientId',
          policyType: [PolicyType.DATA_POLICY, PolicyType.BEHAVIOR_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'topic',
          policyType: [PolicyType.DATA_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'policyId',
          policyType: [PolicyType.DATA_POLICY, PolicyType.BEHAVIOR_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'validationResult',
          policyType: [PolicyType.DATA_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'fromState',
          policyType: [PolicyType.BEHAVIOR_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'toState',
          policyType: [PolicyType.BEHAVIOR_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'triggerEvent',
          policyType: [PolicyType.BEHAVIOR_POLICY],
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'timestamp',
          policyType: [PolicyType.DATA_POLICY, PolicyType.BEHAVIOR_POLICY],
        }),
      ],
    })
  })
})
