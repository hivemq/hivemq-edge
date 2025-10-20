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
  const toastCallMock = vi.fn()
  const isActiveMock = vi.fn()
  const toastMock = Object.assign(toastCallMock, { promise: toastPromiseMock, isActive: isActiveMock })

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
    toastCallMock.mockReset()
    isActiveMock.mockReset()
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

  describe('onError', () => {
    it('should show error toast with Error instance', () => {
      isActiveMock.mockReturnValue(false)
      const { result } = renderHook(() => useBridgeManager(), { wrapper })
      act(() => {
        result.current.onError(new Error('Test error'), { id: 'toast-id' })
      })
      expect(toastCallMock).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: 'Test error',
        })
      )
    })

    it('should show error toast with route error response', () => {
      isActiveMock.mockReturnValue(false)
      const routeError = Object.assign(new Error(), { statusText: 'Not Found', status: 404 })
      const { result } = renderHook(() => useBridgeManager(), { wrapper })
      act(() => {
        result.current.onError(routeError, { id: 'toast-id' })
      })
      expect(toastCallMock).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
        })
      )
      // Just verify the toast was called, as the description can vary
      expect(toastCallMock).toHaveBeenCalledTimes(1)
    })

    it('should show error toast with string error', () => {
      isActiveMock.mockReturnValue(false)
      const { result } = renderHook(() => useBridgeManager(), { wrapper })
      act(() => {
        result.current.onError('String error' as unknown as Error, { id: 'toast-id' })
      })
      expect(toastCallMock).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: 'String error',
        })
      )
    })

    it('should use custom description when provided', () => {
      isActiveMock.mockReturnValue(false)
      const { result } = renderHook(() => useBridgeManager(), { wrapper })
      act(() => {
        result.current.onError(new Error('Test error'), { id: 'toast-id', description: 'Custom description' })
      })
      expect(toastCallMock).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: 'Custom description',
        })
      )
    })

    it('should not show toast if already active with id', () => {
      isActiveMock.mockReturnValue(true)
      const { result } = renderHook(() => useBridgeManager(), { wrapper })
      act(() => {
        result.current.onError(new Error('Test error'), { id: 'toast-id' })
      })
      expect(toastCallMock).not.toHaveBeenCalled()
    })

    it('should show toast if no id provided', () => {
      const { result } = renderHook(() => useBridgeManager(), { wrapper })
      act(() => {
        result.current.onError(new Error('Test error'))
      })
      expect(toastCallMock).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: 'Test error',
        })
      )
    })
  })
})
