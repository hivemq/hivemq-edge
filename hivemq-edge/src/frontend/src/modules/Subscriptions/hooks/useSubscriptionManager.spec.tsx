/// <reference types="cypress" />

import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { useSubscriptionManager } from '@/modules/Subscriptions/hooks/useSubscriptionManager.tsx'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import { MemoryRouter } from 'react-router-dom'
import { ReactFlowProvider } from 'reactflow'
import { Adapter, AdaptersList, Bridge, BridgeList, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { http, HttpResponse } from 'msw'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

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

const TEST_INWARD_EXPECTED = {
  inwardManager: expect.objectContaining({
    formData: expect.objectContaining({
      subscriptions: [
        {
          destination: 'root/topic/ref/1',
          qos: 0,
        },
        {
          destination: 'root/topic/ref/2',
          qos: 0,
        },
      ],
    }),
    schema: expect.objectContaining({
      required: ['subscriptions'],
      type: 'object',
      properties: {
        subscriptions: expect.objectContaining({
          description: 'List of subscriptions for the simulation',
        }),
      },
    }),
    uiSchema: expect.objectContaining({
      'ui:submitButtonOptions': {
        norender: true,
      },
      subscriptions: {
        items: {
          'ui:collapsable': {
            titleKey: 'destination',
          },
          'ui:order': ['destination', 'qos', '*'],
        },
      },
    }),
  }),
  isLoading: false,
}

describe('useSubscriptionManager', () => {
  beforeEach(() => {
    server.use(...customHandlers([mockProtocolAdapter], [mockAdapter], []))
  })

  it('should return no subscription for wrong adapter', async () => {
    const { result } = renderHook(() => useSubscriptionManager('wrong-adapter'), { wrapper })
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

  it('should return inward subscription', async () => {
    const { result } = renderHook(() => useSubscriptionManager(MOCK_ADAPTER_ID), { wrapper })
    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual({ ...TEST_INWARD_EXPECTED, outwardManager: undefined })
  })

  it('should return outward subscription', async () => {
    server.use(
      ...customHandlers(
        [{ ...mockProtocolAdapter, id: 'opc-ua-client' }],
        [{ ...mockAdapter, type: 'opc-ua-client' }],
        []
      )
    )
    const { result } = renderHook(() => useSubscriptionManager(MOCK_ADAPTER_ID), { wrapper })
    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual({
      ...TEST_INWARD_EXPECTED,
      outwardManager: expect.objectContaining({
        formData: {
          subscriptions: expect.arrayContaining([
            expect.objectContaining({
              'mqtt-topic': ['bar/test8', 'pump1/temperature'],
            }),
          ]),
        },
        uiSchema: {
          subscriptions: {
            'ui:field': 'mqtt:transform',
            items: {
              'mqtt-topic': {
                items: {
                  'ui:options': {
                    create: false,
                    multiple: false,
                  },
                },
              },
            },
          },
          'ui:submitButtonOptions': {
            norender: true,
          },
        },
        schema: expect.objectContaining({
          required: ['subscriptions'],
          type: 'object',
          properties: expect.objectContaining({
            subscriptions: expect.objectContaining({
              items: expect.objectContaining({
                required: ['node', 'mqtt-topic'],
              }),
            }),
          }),
        }),
      }),
    })
  })
})
