import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetAllBehaviorFSM } from '@datahub/api/hooks/DataHubFsmService/useGetAllBehaviorFSM.ts'

describe('useGetAllBehaviorFSM', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetAllBehaviorFSM, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        properties: {
          id: {
            description: 'The unique id of this behaviour policy',
            pattern: '^[A-Za-z][A-Za-z0-9._-]{0,1023}$',
            title: 'id',
            type: 'string',
          },
          model: {
            default: 'Mqtt.events',
            enum: ['Publish.quota', 'Mqtt.events', 'Publish.duplicate'],
            title: 'Behavior Model',
          },
        },
        required: ['id', 'model'],
        type: 'object',
      })
    )
  })
})
