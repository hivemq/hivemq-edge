import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers } from './__handlers__'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.ts'
import type { DomainTagList } from '@/api/__generated__'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

describe('useGetDomainTags', () => {
  beforeEach(() => {
    server.use(...deviceHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data for OPCUA', async () => {
    const { result } = renderHook(() => useGetDomainTags(MockAdapterType.OPC_UA), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<DomainTagList>({
      items: [
        {
          definition: {
            node: 'ns=3;i=1002',
          },
          description: 'This is a very long description for the OPCUA tag, just to test the content',
          name: `${MockAdapterType.OPC_UA}/power/off`,
        },
        {
          definition: {
            node: 'ns=3;i=1008',
          },
          name: `${MockAdapterType.OPC_UA}/log/event`,
        },
      ],
    })
  })
  it('should load the data for MODBUS', async () => {
    const { result } = renderHook(() => useGetDomainTags(MockAdapterType.MODBUS), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<DomainTagList>({
      items: [
        {
          definition: {
            endIdx: 1,
            startIdx: 0,
          },
          name: `${MockAdapterType.MODBUS}/alert`,
        },
      ],
    })
  })
  it('should load the data for others', async () => {
    const { result } = renderHook(() => useGetDomainTags(MockAdapterType.SIMULATION), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<DomainTagList>({
      items: [
        {
          definition: {},
          name: `${MockAdapterType.SIMULATION}/log/event`,
        },
      ],
    })
  })
})
