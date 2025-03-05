import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { createErrorHandler, toErrorList } from '@rjsf/utils'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { AdaptersList, Combiner, DataCombining, ProtocolAdaptersList } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import {
  handlers as failCapabilityHandlers,
  mockAdapter_OPCUA,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockCombinerId, mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'

import { useValidateCombiner } from './useValidateCombiner'
import { http, HttpResponse } from 'msw'

const capabilityHandlers = [
  http.get('*/protocol-adapters/types', () => {
    return HttpResponse.json<ProtocolAdaptersList>({ items: [mockProtocolAdapter_OPCUA] }, { status: 200 })
  }),

  http.get('*/protocol-adapters/adapters', () => {
    return HttpResponse.json<AdaptersList>({ items: [mockAdapter_OPCUA] }, { status: 200 })
  }),
]

describe('useValidateCombiner', () => {
  beforeEach(() => {
    server.use(...capabilityHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  const renderValidateHook = async (formData: Combiner | undefined) => {
    const errors = createErrorHandler<Combiner>(formData || mockEmptyCombiner)

    const { result } = renderHook(() => useValidateCombiner([], []), { wrapper })
    await waitFor(() => {
      expect(result.current).not.toBeUndefined()
    })

    const formValidation = result.current?.(formData, errors)
    return toErrorList(formValidation)
  }

  describe('validateSourceCapability', () => {
    it('should fail to validate an empty payload', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook(undefined)
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'The combiner payload must be defined',
        }),
      ])
    })

    it('should fail to validate an unknown adapter', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'dd',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: "The Edge broker must be connected to the combiner's sources",
        }),
        expect.objectContaining({
          message: 'This is not a valid reference to a Workspace entity',
        }),
      ])
    })

    it('should fail to validate an adapter without combine', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'my-adapter',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: "The Edge broker must be connected to the combiner's sources",
        }),
        expect.objectContaining({
          message: 'The adapter does not support data combining and cannot be used as a source',
        }),
      ])
    })

    it('should fail to validate an adapter without combine but validate the Edge', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'the edge name',
              type: EntityType.EDGE_BROKER,
            },
            {
              id: 'my-adapter',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'The adapter does not support data combining and cannot be used as a source',
        }),
      ])
    })

    it('should validate properly', async () => {
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'the edge name',
              type: EntityType.EDGE_BROKER,
            },
            {
              id: 'opcua-1',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([])
    })
  })

  describe('validateDataSources', () => {
    const getFormData = (mappings: DataCombining[]): Combiner => ({
      id: mockCombinerId,
      name: 'my-combiner',
      sources: {
        items: [
          {
            id: 'the edge name',
            type: EntityType.EDGE_BROKER,
          },
          {
            id: 'opcua-1',
            type: EntityType.ADAPTER,
          },
        ],
      },
      mappings: {
        items: mappings,
      },
    })

    it('should validate an empty list of mappings', async () => {
      const errors = await renderValidateHook(getFormData([]))
      expect(errors).toStrictEqual([])
    })
  })

  // TODO[TEST] Complete the test suite
})
