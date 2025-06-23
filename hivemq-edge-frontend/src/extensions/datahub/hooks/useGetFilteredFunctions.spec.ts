import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { BehaviorPolicyTransitionEvent } from '@/api/__generated__'

import { handlers, handlersWithoutLicense } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { useGetFilteredFunction } from '@datahub/hooks/useGetFilteredFunctions.ts'
import { DataHubNodeType } from '@datahub/types.ts'

describe('useGetFilteredFunctions', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  interface TestEachSuite {
    type?: DataHubNodeType
    transition?: BehaviorPolicyTransitionEvent
    omitLicense?: boolean
    expected: string[]
    target: string
  }

  it.each<TestEachSuite>([
    {
      expected: ['Delivery.redirectTo', 'Serdes.serialize', 'Serdes.deserialize', 'DataHub.transform'],
      target: 'no policy type',
    },
    {
      target: 'no license restrictions',
      omitLicense: true,
      expected: [
        'Mqtt.UserProperties.add',
        'Delivery.redirectTo',
        'System.log',
        'Serdes.serialize',
        'Serdes.deserialize',
        'Metrics.Counter.increment',
        'Mqtt.disconnect',
        'Mqtt.drop',
        'DataHub.transform',
      ],
    },
    {
      target: 'behaviour policy',
      type: DataHubNodeType.BEHAVIOR_POLICY,
      expected: [
        'Mqtt.UserProperties.add',
        'System.log',
        'Metrics.Counter.increment',
        'Mqtt.disconnect',
        'Mqtt.drop',
        'DataHub.transform',
      ],
    },
    {
      target: 'behaviour policy and no license',
      omitLicense: true,
      type: DataHubNodeType.DATA_POLICY,
      expected: [
        'Mqtt.UserProperties.add',
        'Delivery.redirectTo',
        'System.log',
        'Serdes.serialize',
        'Serdes.deserialize',
        'Metrics.Counter.increment',
        'Mqtt.disconnect',
        'Mqtt.drop',
        'DataHub.transform',
      ],
    },
  ])('should returns $expected with $target', async ({ type, transition, omitLicense, expected }) => {
    server.use(...(omitLicense ? handlersWithoutLicense : handlers))

    const { result } = renderHook(() => useGetFilteredFunction(type, transition), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    const functionId = result.current.data.map((specs) => specs.functionId)
    expect(functionId).toStrictEqual(expected)
  })
})
