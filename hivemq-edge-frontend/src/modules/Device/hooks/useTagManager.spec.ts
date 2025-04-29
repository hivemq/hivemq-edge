import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import '@/config/i18n.config.ts'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import type { Adapter, AdaptersList, DomainTagList, ProtocolAdapter, ProtocolAdaptersList } from '@/api/__generated__'
import {
  MOCK_DEVICE_TAGS,
  mockAdapter,
  mockAdapter_OPCUA,
  mockProtocolAdapter,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers } from '@/api/hooks/useDomainModel/__handlers__'

import { useTagManager } from '@/modules/Device/hooks/useTagManager.ts'
import { MockAdapterType } from '../../../__test-utils__/adapters/types'
import type { RJSFSchema } from '@rjsf/utils'

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
    // TODO[NVL] Not the most obvious reuse system!
    const [tagSchemasHandler] = handlers

    server.use(...customHandlers([mockProtocolAdapter], [mockAdapter]), tagSchemasHandler)
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
          formData: {
            items: [],
          },
          schema: undefined,
          uiSchema: expect.objectContaining({}),
        },
        data: {
          items: [],
        },
        error: 'Internal Server Error',
        isError: true,
        isLoading: false,
        isPending: false,
        capabilities: undefined,
      })
    )
  })

  it.skip('should return errors for missing tags', async () => {
    const { result } = renderHook(() => useTagManager(MOCK_ADAPTER_ID), { wrapper })
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
          schema: undefined,
          uiSchema: expect.objectContaining({}),
        },
        data: {
          items: [],
        },
        error: 'Internal Server Error',
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

    expect(result.current.context.formData).toStrictEqual({
      items: [],
    })

    expect(result.current.context.schema).not.toBeUndefined()

    const { definitions, properties } = result.current.context.schema as RJSFSchema
    expect(definitions).toStrictEqual({
      TagSchema: expect.objectContaining({
        properties: expect.objectContaining({
          definition: expect.objectContaining({
            description: 'The actual definition of the tag on the device',
          }),
          description: expect.objectContaining({
            description: 'A human readable description of the tag',
          }),
          name: expect.objectContaining({
            description: 'name of the tag to be used in mappings',
          }),
          protocolId: expect.objectContaining({
            const: 'opcua',
          }),
        }),
      }),
    })
    expect(properties).toStrictEqual({
      items: expect.objectContaining({
        description: 'The list of all tags defined in the device',
      }),
    })

    expect(result.current.context.uiSchema).toStrictEqual(
      expect.objectContaining({
        items: expect.objectContaining({
          'ui:field': expect.anything(),
          items: expect.objectContaining({
            protocolId: {
              'ui:widget': 'hidden',
            },
          }),
        }),
        'ui:submitButtonOptions': expect.anything(),
      })
    )

    expect(result.current.data).toStrictEqual({ items: [] })
    expect(result.current.isError).toStrictEqual(false)
    expect(result.current.isLoading).toStrictEqual(false)
    expect(result.current.isPending).toStrictEqual(false)
    expect(result.current.error).toBeUndefined()
    expect(result.current.onCreate).toBeTypeOf('function')
    expect(result.current.onDelete).toBeTypeOf('function')
    expect(result.current.onUpdate).toBeTypeOf('function')
    expect(result.current.onupdateCollection).toBeTypeOf('function')
    expect(result.current.capabilities).toStrictEqual(['READ', 'DISCOVER', 'WRITE', 'COMBINE'])
  })

  it('should do it properly', async () => {
    server.use(...customHandlers([mockProtocolAdapter_OPCUA], [mockAdapter_OPCUA], { items: [] }))
    const { result } = renderHook(() => useTagManager('opcua-1'), { wrapper })
    expect(result.current.isLoading).toBeTruthy()

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    // TODO[TEST] Needs to test the effect of calling the functions
    act(() => {
      result.current.onDelete('test-id')
    })

    act(() => {
      result.current.onCreate(MOCK_DEVICE_TAGS('test-id', MockAdapterType.OPC_UA)[0])
    })

    act(() => {
      result.current.onUpdate('test-id', MOCK_DEVICE_TAGS('test-id', MockAdapterType.OPC_UA)[0])
    })

    act(() => {
      result.current.onupdateCollection({ items: MOCK_DEVICE_TAGS('test-id', MockAdapterType.OPC_UA) })
    })
  })
})
