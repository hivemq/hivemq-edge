import { expect } from 'vitest'
import { renderHook } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { usePrivateMqttClient } from '@/hooks/usePrivateMqttClient/usePrivateMqttClient.ts'
import { PrivateMqttClientType } from '@/hooks/usePrivateMqttClient/type.ts'

describe('usePrivateMqttClient', () => {
  it('should load the data', async () => {
    const { result } = renderHook(() => usePrivateMqttClient(), { wrapper })

    expect(result.current).toStrictEqual<PrivateMqttClientType>({ state: undefined, actions: undefined })
  })
})
