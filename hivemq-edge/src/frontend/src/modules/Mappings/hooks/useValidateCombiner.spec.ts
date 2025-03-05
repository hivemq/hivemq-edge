import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { createErrorHandler, toErrorList } from '@rjsf/utils'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { Combiner } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { handlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockCombinerId, mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'

import { useValidateCombiner } from './useValidateCombiner'

describe('useValidateCombiner', () => {
  beforeEach(() => {
    server.use(...handlers)
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

    const formValidation = result.current(formData, errors)
    return toErrorList(formValidation)
  }

  it('should validate an empty payload', async () => {
    const errors = await renderValidateHook(undefined)
    expect(errors).toStrictEqual([
      expect.objectContaining({
        message: 'The combiner payload must be defined',
      }),
    ])
  })

  it('should validate capability', async () => {
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
})
