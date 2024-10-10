/// <reference types="cypress" />

import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ReactFlowProvider } from 'reactflow'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import {
  mockAdapter,
  mockAdapter_OPCUA,
  mockProtocolAdapter,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { Adapter, AdaptersList, type DomainTagList, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import { useTagManager } from '@/modules/Mappings/hooks/useTagManager.tsx'
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
  tags?: DomainTagList
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

  http.get('**/protocol-adapters/adapters/**/tags', () => {
    return tags
      ? HttpResponse.json<DomainTagList>(tags, { status: 200 })
      : new HttpResponse(null, {
          status: 500,
        })
  }),
]

describe('useTagManager', () => {
  beforeEach(() => {
    server.use(...customHandlers([mockProtocolAdapter], [mockAdapter]))
  })

  it('should return errors for wrong adapter', async () => {
    const { result } = renderHook(() => useTagManager('wrong-adapter'), { wrapper })
    expect(result.current.isLoading).toBeTruthy()

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual(
      expect.objectContaining({
        context: {
          formData: undefined,
          schema: undefined,
          uiSchema: {},
        },
        data: undefined,
        error: 'The protocol adapter for this device cannot be found',
        isError: true,
        isLoading: false,
        isPending: false,
      })
    )
  })

  it('should return errors for missing tags', async () => {
    const { result } = renderHook(() => useTagManager(MOCK_ADAPTER_ID), { wrapper })
    expect(result.current.isLoading).toBeTruthy()

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual(
      expect.objectContaining({
        context: {
          formData: undefined,
          schema: undefined,
          uiSchema: {},
        },
        data: undefined,
        error: 'The form cannot be created, due to internal errors',

        isError: true,
        isLoading: false,
        isPending: false,
      })
    )
  })

  it('should return the manager', async () => {
    server.use(...customHandlers([mockProtocolAdapter_OPCUA], [mockAdapter_OPCUA], { items: [] }))
    const { result } = renderHook(() => useTagManager('opcua-1'), { wrapper })
    expect(result.current.isLoading).toBeTruthy()

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual(
      expect.objectContaining({
        context: {
          formData: {
            items: [],
          },
          schema: expect.objectContaining({
            definitions: expect.objectContaining({
              DeviceDataPoint: expect.objectContaining({
                properties: expect.objectContaining({
                  node: expect.objectContaining({
                    title: 'Source Node ID',
                  }),
                }),
              }),
              DomainTag: expect.objectContaining({}),
            }),
          }),
          uiSchema: {},
        },
        data: {
          items: [],
        },
        error: null,
        isError: false,
        isLoading: false,
        isPending: false,
      })
    )
  })
})
