import { renderHook, act } from '@testing-library/react'
import { describe, expect, vi } from 'vitest'
import type { MouseEvent } from 'react'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { useContextMenu } from './useContextMenu.ts'

const mocks = vi.hoisted(() => {
  return {
    navigate: vi.fn(() => undefined),
  }
})

describe('useContextMenu', () => {
  beforeEach(() => {
    window.localStorage.clear()

    vi.mock('react-router-dom', async () => {
      const actual = await vi.importActual<object>('react-router-dom')
      return {
        ...actual,
        useNavigate() {
          return mocks.navigate
        },
      }
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should navigate to the proper route', () => {
    const preventDefault = vi.fn()

    const { result } = renderHook(() => useContextMenu('id', true, 'route'), { wrapper })

    act(() => {
      const mockEvent = { preventDefault: preventDefault } as unknown as MouseEvent<HTMLElement>
      result.current.onContextMenu(mockEvent)
    })

    expect(preventDefault).toHaveBeenCalled()
    expect(mocks.navigate).toHaveBeenCalledWith('route/id')
  })

  it('should not navigate if not selected', () => {
    const preventDefault = vi.fn()

    const { result } = renderHook(() => useContextMenu('id', false, 'route'), { wrapper })

    act(() => {
      const mockEvent = { preventDefault: preventDefault } as unknown as MouseEvent<HTMLElement>
      result.current.onContextMenu(mockEvent)
    })

    expect(preventDefault).not.toHaveBeenCalled()
    expect(mocks.navigate).not.toHaveBeenCalled()
  })
})
