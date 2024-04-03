/// <reference types="cypress" />

import { expect } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { rest } from 'msw'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_ADAPTER_MODBUS, MOCK_PROTOCOL_MODBUS } from '@/__test-utils__/adapters/modbus.ts'

import { Adapter, AdaptersList, Bridge, BridgeList, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { EdgeTopicsOptions, useGetEdgeTopics, reduceTopicsBy } from './useGetEdgeTopics.tsx'

interface Suite {
  topic: string
  option: EdgeTopicsOptions
  expected: string[]
}

const reducerSuite: Suite[] = [
  { topic: 'test/123', expected: ['test/123'], option: { publishOnly: true } },
  { topic: 'test/123', expected: ['test/123'], option: { publishOnly: false } },
  { topic: 'test/#', expected: [], option: { publishOnly: true } },
  { topic: 'test/#', expected: ['test/#'], option: { publishOnly: false } },
  { topic: 'test/+/123', expected: [], option: { publishOnly: true } },
  { topic: 'test/+/123', expected: ['test/+/123'], option: { publishOnly: false } },
  { topic: 'test', expected: [], option: { branchOnly: true } },
  { topic: 'test/123', expected: ['test'], option: { branchOnly: true } },
  { topic: 'test/123', expected: ['test/123'], option: { branchOnly: false } },
  { topic: 'test/#', expected: ['test'], option: { branchOnly: true } },
  { topic: 'test/#', expected: ['test/#'], option: { branchOnly: false } },
  { topic: 'test/+/123', expected: ['test/+'], option: { branchOnly: true } },
  { topic: 'test/+/123', expected: ['test/+/123'], option: { branchOnly: false } },
]

describe('reduceTopicsBy', () => {
  it.each<Suite>(reducerSuite)('$topic with $option returns $expected', ({ topic, option, expected }) => {
    expect(reduceTopicsBy(option)([], topic)).toMatchObject(expected)
  })
})

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
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

const customHandlers = (
  types: Array<ProtocolAdapter> | undefined,
  adapters?: Array<Adapter> | undefined,
  bridges?: Array<Bridge> | undefined
) => [
  rest.get('**/protocol-adapters/types', (_, res, ctx) => {
    return types ? res(ctx.json<ProtocolAdaptersList>({ items: types }), ctx.status(200)) : res(ctx.status(500))
  }),
  rest.get('**/protocol-adapters/adapters', (_, res, ctx) => {
    return adapters ? res(ctx.json<AdaptersList>({ items: adapters }), ctx.status(200)) : res(ctx.status(500))
  }),
  rest.get('**/management/bridges', (_, res, ctx) => {
    return bridges ? res(ctx.json<BridgeList>({ items: bridges }), ctx.status(200)) : res(ctx.status(500))
  }),
]

describe('useGetEdgeTopics', () => {
  it('should return basic payload', async () => {
    server.use(...customHandlers([], [], []))

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual({
      data: [],
      error: null,
      isError: false,
      isLoading: false,
      isSuccess: true,
    })
  })

  it("should return bridge's topics", async () => {
    server.use(...customHandlers([], [], [mockBridge]))
    server.printHandlers()

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: ['root/topic/act/1'],
      })
    )
  })

  it("should return bridge's topics for publishing and subscribing", async () => {
    server.use(...customHandlers([], [], [mockBridge]))
    server.printHandlers()

    const { result } = renderHook(() => useGetEdgeTopics({ publishOnly: false }), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: ['#', 'root/topic/act/1'],
      })
    )
  })

  it("should return adapters' topics", async () => {
    server.use(
      ...customHandlers(
        [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_MODBUS],
        [MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_MODBUS as Adapter],
        [mockBridge]
      )
    )
    server.printHandlers()

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: [
          'a/valid/topic/modbus/1',
          'a/valid/topic/opc-ua-client/1',
          'a/valid/topic/opc-ua-client/2',
          'root/topic/act/1',
        ],
      })
    )
  })
})
