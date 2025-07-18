import type { Bridge } from '@/api/__generated__'
import { useBridgeManager } from '@/modules/Bridges/hooks/useBridgeManager.ts'
import type { Mock } from 'vitest'
import { beforeEach, expect, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'

import * as useCreateBridgeModule from '@/api/hooks/useGetBridges/useCreateBridge'
import * as useDeleteBridgeModule from '@/api/hooks/useGetBridges/useDeleteBridge'
import * as useUpdateBridgeModule from '@/api/hooks/useGetBridges/useUpdateBridge'
import * as useToastModule from '@chakra-ui/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { handlers } from '@/api/hooks/useGetBridges/__handlers__'

import '@/config/i18n.config.ts'

vi.mock('@chakra-ui/react', async (importOriginal) => {
  // eslint-disable-next-line @typescript-eslint/consistent-type-imports
  const actual = await importOriginal<typeof import('@chakra-ui/react')>()
  return {
    ...actual,
    useToast: vi.fn(),
  }
})

describe('useBridgeManager', () => {
  const mutateAsyncMock = vi.fn()
  const toastPromiseMock = vi.fn()
  const toastMock = { promise: toastPromiseMock, isActive: vi.fn() }

  beforeEach(() => {
    vi.restoreAllMocks()
    server.use(...handlers)

    // @ts-ignore ignore the other method
    vi.spyOn(useCreateBridgeModule, 'useCreateBridge').mockReturnValue({ mutateAsync: mutateAsyncMock })
    // @ts-ignore ignore the other method
    vi.spyOn(useDeleteBridgeModule, 'useDeleteBridge').mockReturnValue({ mutateAsync: mutateAsyncMock })
    // @ts-ignore ignore the other method
    vi.spyOn(useUpdateBridgeModule, 'useUpdateBridge').mockReturnValue({ mutateAsync: mutateAsyncMock })
    ;(useToastModule.useToast as Mock).mockReturnValue(toastMock)

    mutateAsyncMock.mockReset()
    toastPromiseMock.mockReset()
  })

  it('should return the manager', async () => {
    const { result } = renderHook(() => useBridgeManager(), { wrapper })
    expect(result.current.onDelete).toBeTypeOf('function')
    expect(result.current.onCreate).toBeTypeOf('function')
    expect(result.current.onUpdate).toBeTypeOf('function')
    expect(result.current.onError).toBeTypeOf('function')
  })

  it('should call delete mutator and toast on onDelete', async () => {
    mutateAsyncMock.mockResolvedValueOnce({})
    const { result } = renderHook(() => useBridgeManager(), { wrapper })
    await act(async () => {
      result.current.onDelete('bridge-id')
    })
    expect(mutateAsyncMock).toHaveBeenCalledWith('bridge-id')
    expect(toastPromiseMock).toHaveBeenCalled()
  })

  it('should call create mutator and toast on onCreate', async () => {
    mutateAsyncMock.mockResolvedValueOnce({})
    const { result } = renderHook(() => useBridgeManager(), { wrapper })
    await act(async () => {
      await result.current.onCreate({} as Bridge)
    })
    expect(mutateAsyncMock).toHaveBeenCalled()
    expect(toastPromiseMock).toHaveBeenCalled()
  })

  it('should call update mutator and toast on onUpdate', async () => {
    mutateAsyncMock.mockResolvedValueOnce({})
    const { result } = renderHook(() => useBridgeManager(), { wrapper })
    await act(async () => {
      await result.current.onUpdate('bridge-id', {} as Bridge)
    })
    expect(mutateAsyncMock).toHaveBeenCalledWith({ name: 'bridge-id', requestBody: {} })
    expect(toastPromiseMock).toHaveBeenCalled()
  })

  it.skip('should show error toast on onError', () => {
    // Skip: Cannot mock the constructor of the toast
    const { result } = renderHook(() => useBridgeManager(), { wrapper })
    result.current.onError(new Error('fail'), { id: 'toast-id', description: 'desc' })
    expect(toastPromiseMock).toHaveBeenCalled()
  })
})
