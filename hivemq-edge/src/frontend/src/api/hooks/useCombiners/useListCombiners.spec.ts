import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'

import { useListCombiners } from './useListCombiners'
import type { CombinerList } from '../../__generated__'

describe('useListCombiners', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useListCombiners(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<CombinerList>({
      items: [
        {
          id: '6991ff43-9105-445f-bce3-976720df40a3',
          name: 'my-combiner',
        },
      ],
    })
  })
})
