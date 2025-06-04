import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetAllFunctions } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctions.ts'

describe('useGetAllFunctions', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetAllFunctions, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.arrayContaining([
        expect.objectContaining({
          description: 'Logs a message on the given level',
          metadata: {
            hasArguments: false,
            isDataOnly: false,
            isTerminal: false,
          },
          properties: {
            level: {
              description: 'Specifies the log level of the function in the hivemq.log file',
              enum: ['DEBUG', 'ERROR', 'WARN', 'INFO', 'TRACE'],
              title: 'Log Level',
              type: 'string',
            },
            message: {
              description:
                'Adds a user-defined string that prints to the log file. For more information, see Example log message',
              title: 'Message',
              type: 'string',
            },
          },
          required: ['level', 'message'],
          title: 'System.log',
          type: 'object',
        }),
        expect.objectContaining({
          description: 'Redirects an MQTT PUBLISH message to a specified topic',
        }),
        expect.objectContaining({
          description: 'Adds a user property to the MQTT message',
        }),
        expect.objectContaining({
          description:
            'Deserializes a binary MQTT message payload into a data object based on the configured JSON Schema or Protobuf schema.',
        }),
        expect.objectContaining({
          description:
            'Serializes a data object into a binary MQTT message payload based on the configured JSON Schema or Protobuf schema.',
        }),
        expect.objectContaining({
          description: 'Increments a metric of type counter, which can be accessed with monitoring',
        }),
        expect.objectContaining({
          description: 'Redirects an MQTT PUBLISH message to a specified topic',
        }),
      ])
    )
  })
})
