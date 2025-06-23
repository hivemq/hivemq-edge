import { expect, describe, it, afterEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { BehaviorPolicyTransitionEvent } from '@/api/__generated__'

import { handlers, handlersWithoutLicense } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { useFilteredFunctionsFetcher } from '@datahub/hooks/useFilteredFunctionsFetcher.ts'
import { DataHubNodeType } from '@datahub/types.ts'

describe('useGetFilteredFunctionsFetcher', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should return a getFilteredFunctions function and status properties', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useFilteredFunctionsFetcher(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toHaveProperty('getFilteredFunctions')
    expect(typeof result.current.getFilteredFunctions).toBe('function')
    expect(result.current).toHaveProperty('isLoading', false)
    expect(result.current).toHaveProperty('isError', false)
    expect(result.current).toHaveProperty('error', null)
    expect(result.current).toHaveProperty('isSuccess', true)
  })

  it('should filter by license allowance', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useFilteredFunctionsFetcher(), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    // Compare with and without license restriction
    server.use(...handlersWithoutLicense)

    const { result: resultWithoutLicense } = renderHook(() => useFilteredFunctionsFetcher(), { wrapper })

    await waitFor(() => {
      expect(resultWithoutLicense.current.isSuccess).toBeTruthy()
    })

    const filteredWithLicense = result.current.getFilteredFunctions()
    const filteredWithoutLicense = resultWithoutLicense.current.getFilteredFunctions()

    expect(filteredWithLicense.length).toBeLessThan(filteredWithoutLicense.length)
  })

  it('should filter by transition event when specified', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useFilteredFunctionsFetcher(), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    const allFunctions = result.current.getFilteredFunctions()
    const filteredByEvent = result.current.getFilteredFunctions(
      DataHubNodeType.DATA_POLICY,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH
    )

    expect(filteredByEvent.length).toEqual(allFunctions.length)

    // Check that all returned functions support the specified event
    filteredByEvent.forEach((func) => {
      if (func.metadata.supportedEvents?.length) {
        expect(func.metadata.supportedEvents).toContain(BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH)
      }
    })
  })

  it('should filter data-only functions for non-data-policy types by default', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useFilteredFunctionsFetcher(), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    const dataPolicyFunctions = result.current.getFilteredFunctions(DataHubNodeType.DATA_POLICY)
    const behaviorPolicyFunctions = result.current.getFilteredFunctions(DataHubNodeType.BEHAVIOR_POLICY)

    // Behavior policy should have fewer functions than data policy by default
    expect(behaviorPolicyFunctions.length).toBeLessThan(dataPolicyFunctions.length)
  })

  interface TestEachSuite {
    type?: DataHubNodeType
    transition?: BehaviorPolicyTransitionEvent
    omitLicense?: boolean
    isTransformIncluded?: boolean
    expectedMinLength: number
    target: string
  }

  it.each<TestEachSuite>([
    {
      target: 'default parameters',
      expectedMinLength: 4,
      isTransformIncluded: true,
    },
    {
      target: 'behavior policy type',
      type: DataHubNodeType.BEHAVIOR_POLICY,
      expectedMinLength: 3,
    },
    {
      target: 'data policy type',
      type: DataHubNodeType.DATA_POLICY,
      expectedMinLength: 4,
      isTransformIncluded: true,
    },
    {
      target: 'with specific transition event',
      type: DataHubNodeType.BEHAVIOR_POLICY,
      transition: BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
      expectedMinLength: 2,
    },
    {
      target: 'without license restrictions',
      omitLicense: true,
      expectedMinLength: 8,
      isTransformIncluded: true,
    },
  ])(
    'should return expected functions with $target',
    async ({ type, transition, omitLicense, expectedMinLength, isTransformIncluded }) => {
      server.use(...(omitLicense ? handlersWithoutLicense : handlers))

      const { result } = renderHook(() => useFilteredFunctionsFetcher(), { wrapper })

      await waitFor(() => {
        expect(result.current.isLoading).toBeFalsy()
        expect(result.current.isSuccess).toBeTruthy()
      })

      const filteredFunctions = result.current.getFilteredFunctions(type, transition)
      expect(filteredFunctions.length).toBeGreaterThanOrEqual(expectedMinLength)

      // Verify DataHub.transform is included
      const transformFunction = filteredFunctions.find((f) => f.functionId === 'DataHub.transform')
      if (isTransformIncluded) expect(transformFunction).toBeDefined()
      else expect(transformFunction).toBeUndefined()
    }
  )
})
