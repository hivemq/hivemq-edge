/// <reference types="cypress" />

import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ReactFlowProvider } from 'reactflow'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { Adapter, AdaptersList, Bridge, BridgeList, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import { useMappingManager } from '@/modules/Mappings/hooks/useMappingManager.tsx'
import { MappingManagerType } from '@/modules/Mappings/types.ts'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider
    client={
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: false,
          },
        },
      })
    }
  >
    <AuthProvider>
      <ReactFlowProvider>
        <MemoryRouter>{children}</MemoryRouter>
      </ReactFlowProvider>
    </AuthProvider>
  </QueryClientProvider>
)

const customHandlers = (
  types: Array<ProtocolAdapter> | undefined,
  adapters?: Array<Adapter> | undefined,
  bridges?: Array<Bridge> | undefined
) => [
  http.get('**/protocol-adapters/types', () => {
    return types
      ? HttpResponse.json<ProtocolAdaptersList>({ items: types }, { status: 200 })
      : new HttpResponse(null, { status: 500 })
  }),

  http.get('**/protocol-adapters/adapters', () => {
    return adapters
      ? HttpResponse.json<AdaptersList>({ items: adapters }, { status: 200 })
      : new HttpResponse(null, {
          status: 500,
        })
  }),
  http.get('**/management/bridges', () => {
    return bridges
      ? HttpResponse.json<BridgeList>({ items: bridges }, { status: 200 })
      : new HttpResponse(null, {
          status: 500,
        })
  }),
]

describe('useMappingManager', () => {
  beforeEach(() => {
    server.use(...customHandlers([mockProtocolAdapter], [mockAdapter], []))
  })

  it('should return no mapping specs for wrong adapter', async () => {
    const { result } = renderHook(() => useMappingManager('wrong-adapter'), { wrapper })
    expect(result.current.isLoading).toBeTruthy()

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual({
      inwardManager: undefined,
      isLoading: false,
      outwardManager: undefined,
    })
  })

  it('should return inward mapping specs', async () => {
    const { result } = renderHook(() => useMappingManager(MOCK_ADAPTER_ID), { wrapper })
    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    const { inwardManager, isLoading } = result.current
    expect(isLoading).toBeFalsy()
    expect(inwardManager).not.toBeUndefined()

    const { formData, schema, uiSchema } = inwardManager as MappingManagerType
    expect(formData).not.toBeUndefined()
    expect(formData.simulationToMqtt).toStrictEqual(
      expect.objectContaining({
        simulationToMqttMappings: [
          {
            mqttTopic: 'root/topic/ref/1',
            qos: 0,
          },
          {
            mqttTopic: 'root/topic/ref/2',
            qos: 0,
          },
        ],
      })
    )

    expect(schema).not.toBeUndefined()
    expect(schema.properties).toStrictEqual(
      expect.objectContaining({
        simulationToMqtt: expect.objectContaining({
          properties: expect.objectContaining({
            maxPollingErrorsBeforeRemoval: expect.objectContaining({}),
            pollingIntervalMillis: expect.objectContaining({}),
            simulationToMqttMappings: expect.objectContaining({
              items: expect.objectContaining({
                properties: expect.objectContaining({
                  includeTagNames: expect.objectContaining({}),
                  includeTimestamp: expect.objectContaining({}),
                  messageHandlingOptions: expect.objectContaining({}),
                  mqttQos: expect.objectContaining({}),
                  mqttTopic: expect.objectContaining({}),
                  mqttUserProperties: expect.objectContaining({}),
                }),
              }),
            }),
          }),
        }),
      })
    )
    expect(uiSchema).not.toBeUndefined()
    expect(uiSchema).toStrictEqual(
      expect.objectContaining({
        'ui:submitButtonOptions': expect.objectContaining({ norender: true }),
        id: expect.objectContaining({ 'ui:disabled': false }),
        simulationToMqtt: expect.objectContaining({
          simulationToMqttMappings: expect.objectContaining({
            'ui:batchMode': true,
            items: expect.objectContaining({
              'ui:order': expect.arrayContaining(['mqttTopic']),
              'ui:collapsable': { titleKey: 'mqttTopic' },
            }),
          }),
        }),
      })
    )
  })

  it('should return outward mapping specs', async () => {
    server.use(
      ...customHandlers(
        [{ ...mockProtocolAdapter, id: MockAdapterType.OPC_UA }],
        [{ ...mockAdapter, type: MockAdapterType.OPC_UA }],
        []
      )
    )
    const { result } = renderHook(() => useMappingManager(MOCK_ADAPTER_ID), { wrapper })
    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    const { outwardManager, isLoading } = result.current
    expect(isLoading).toBeFalsy()
    expect(outwardManager).not.toBeUndefined()

    const { formData, schema, uiSchema } = outwardManager as MappingManagerType
    expect(formData).not.toBeUndefined()
    expect(schema).not.toBeUndefined()
    expect(uiSchema).not.toBeUndefined()

    // TODO[NVL] Tests to be continued when not dealing with a mock
  })
})
