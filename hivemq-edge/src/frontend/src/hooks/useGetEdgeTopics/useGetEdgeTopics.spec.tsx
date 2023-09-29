/// <reference types="cypress" />

import { expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

import { QueryClientProvider } from '@tanstack/react-query'
import queryClient from '@/api/queryClient.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { publishOnlyFilter, EdgeTopicsOptions, useGetEdgeTopics } from './useGetEdgeTopics.tsx'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_ADAPTER_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { Adapter } from '@/api/__generated__'
import { MOCK_ADAPTER_MODBUS } from '@/__test-utils__/adapters/modbus.ts'

interface Suite {
  topic: string
  option: EdgeTopicsOptions
  expected: boolean
}

const validationSuite: Suite[] = [
  { topic: 'test/123', expected: true, option: { publishOnly: true } },
  { topic: 'test/123', expected: true, option: { publishOnly: false } },
  { topic: 'test/#', expected: false, option: { publishOnly: true } },
  { topic: 'test/#', expected: true, option: { publishOnly: false } },
  { topic: 'test/+/123', expected: false, option: { publishOnly: true } },
  { topic: 'test/+/123', expected: true, option: { publishOnly: false } },
]

describe('publishOnlyFilter', () => {
  it.each<Suite>(validationSuite)('$topic with $option returns $expected', ({ topic, option, expected }) => {
    expect(publishOnlyFilter(option)(topic)).toMatchObject(expected)
  })
})

const { useGetAdapterTypes, useListProtocolAdapters, useListBridges } = vi.hoisted(() => {
  const defaults = { data: undefined, isLoading: false, isError: false, error: undefined }
  return {
    useGetAdapterTypes: vi.fn().mockReturnValue(defaults),
    useListProtocolAdapters: vi.fn().mockReturnValue(defaults),
    useListBridges: vi.fn().mockReturnValue(defaults),
  }
})

vi.mock('@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx', () => {
  return { useGetAdapterTypes: useGetAdapterTypes }
})

vi.mock('@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx', () => {
  return { useListProtocolAdapters: useListProtocolAdapters }
})

vi.mock('@/api/hooks/useGetBridges/useListBridges.tsx', () => {
  return { useListBridges: useListBridges }
})

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)

describe('useGetEdgeTopics', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('should return basic payload', () => {
    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })

    expect(result.current).toStrictEqual({
      data: [],
      error: undefined,
      isError: false,
      isLoading: false,
    })
  })

  it("should return bridge's topics", () => {
    useListBridges.mockReturnValue({
      data: [mockBridge, mockBridge, mockBridge],
    })

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })

    expect(result.current).toStrictEqual({
      data: ['root/topic/act/1'],
      error: undefined,
      isError: false,
      isLoading: false,
    })
  })

  it("should return bridge's topics for publishing and subscribing", () => {
    useListBridges.mockReturnValue({
      data: [mockBridge, mockBridge, mockBridge],
    })
    useListProtocolAdapters.mockReturnValue({
      data: [],
    })

    const { result } = renderHook(() => useGetEdgeTopics({ publishOnly: false }), { wrapper })

    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: ['#', 'root/topic/act/1'],
      })
    )
  })

  it("should return adapters' topics", () => {
    useListBridges.mockReturnValue({
      data: [mockBridge, mockBridge, mockBridge],
    })
    useListProtocolAdapters.mockReturnValue({
      data: [MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_OPC_UA as Adapter, MOCK_ADAPTER_MODBUS as Adapter],
    })

    const { result } = renderHook(() => useGetEdgeTopics(), { wrapper })

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
