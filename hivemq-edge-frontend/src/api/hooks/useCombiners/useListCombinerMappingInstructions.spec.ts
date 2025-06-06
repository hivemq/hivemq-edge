import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'

import type { Instruction } from '../../__generated__'
import { useListCombinerMappingInstructions } from './useListCombinerMappingInstructions'

describe('useListCombinerMappingInstructions', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useListCombinerMappingInstructions('combinerId', 'mappingId'), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<Array<Instruction>>([])
  })
})
