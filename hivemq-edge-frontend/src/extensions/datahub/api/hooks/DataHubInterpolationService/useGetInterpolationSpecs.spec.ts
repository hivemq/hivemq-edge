import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import type { InterpolationVariable, InterpolationVariableList } from './__handlers__'
import { handlers } from './__handlers__'
import { useGetInterpolationSpecs } from '@datahub/api/hooks/DataHubInterpolationService/useGetInterpolationSpecs.ts'

describe('useGetInterpolationSpecs', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetInterpolationSpecs, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<InterpolationVariableList>({
      items: [
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'clientId',
          isBehaviorPolicy: true,
          isDataPolicy: true,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'topic',
          isBehaviorPolicy: false,
          isDataPolicy: true,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'policyId',
          isBehaviorPolicy: true,
          isDataPolicy: true,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'validationResult',
          isBehaviorPolicy: false,
          isDataPolicy: true,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'fromState',
          isBehaviorPolicy: true,
          isDataPolicy: false,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'toState',
          isBehaviorPolicy: true,
          isDataPolicy: false,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'triggerEvent',
          isBehaviorPolicy: true,
          isDataPolicy: false,
        }),
        expect.objectContaining<Partial<InterpolationVariable>>({
          variable: 'timestamp',
          isBehaviorPolicy: true,
          isDataPolicy: true,
        }),
      ],
    })
  })
})
