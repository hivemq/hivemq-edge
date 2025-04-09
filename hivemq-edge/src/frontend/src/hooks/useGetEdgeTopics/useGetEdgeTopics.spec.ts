import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_ADAPTER_MODBUS, MOCK_PROTOCOL_MODBUS } from '@/__test-utils__/adapters/modbus.ts'

import type {
  Adapter,
  AdaptersList,
  Bridge,
  BridgeList,
  ProtocolAdapter,
  ProtocolAdaptersList,
} from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

import type { EdgeTopicsOptions } from './useGetEdgeTopics.ts'
import { useGetEdgeTopics, reduceTopicsBy } from './useGetEdgeTopics.ts'

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
    server.listHandlers()

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: ['root/topic/ref/1'],
      })
    )
  })

  it("should return bridge's topics for publishing and subscribing", async () => {
    server.use(...customHandlers([], [], [mockBridge]))
    server.listHandlers()

    const { result } = renderHook(() => useGetEdgeTopics({ publishOnly: false }), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: ['#', 'root/topic/ref/1'],
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
    server.listHandlers()

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: ['root/topic/ref/1'],
      })
    )
  })
})
