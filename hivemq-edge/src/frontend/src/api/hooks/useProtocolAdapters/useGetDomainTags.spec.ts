import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers } from './__handlers__'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
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
          protocolId: MockAdapterType.OPC_UA,
          tagDefinition: {
            node: 'ns=3;i=1002',
          },
          tagName: `${MockAdapterType.OPC_UA}/power/off`,
        },
        {
          protocolId: MockAdapterType.OPC_UA,
          tagDefinition: {
            node: 'ns=3;i=1008',
          },
          tagName: `${MockAdapterType.OPC_UA}/log/event`,
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
          protocolId: MockAdapterType.MODBUS,
          tagDefinition: {
            endIdx: 1,
            startIdx: 0,
          },
          tagName: `${MockAdapterType.MODBUS}/alert`,
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
          protocolId: MockAdapterType.SIMULATION,
          tagDefinition: {},
          tagName: `${MockAdapterType.SIMULATION}/log/event`,
        },
      ],
    })
  })
})
