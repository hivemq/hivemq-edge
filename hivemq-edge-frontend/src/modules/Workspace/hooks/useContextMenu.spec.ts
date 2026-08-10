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

// Hoisted by Vitest regardless of where it is written; keep it at the top level so the file reads
// the way it actually executes (nesting it is a warning in Vitest 4 and an error later).
vi.mock('react-router', async () => {
  const actual = await vi.importActual<object>('react-router')
  return {
    ...actual,
    useNavigate() {
      return mocks.navigate
    },
  }
})

describe('useContextMenu', () => {
  beforeEach(() => {
    window.localStorage.clear()
    // Vitest 4's restoreAllMocks only restores spies, so a plain vi.fn() keeps its calls between
    // tests unless it is cleared explicitly.
    mocks.navigate.mockClear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should navigate to the proper route', () => {
    const preventDefault = vi.fn()

    const { result } = renderHook(() => useContextMenu('id', true, 'route/my-id'), { wrapper })

    act(() => {
      const mockEvent = { preventDefault: preventDefault } as unknown as MouseEvent<HTMLElement>
      result.current.onContextMenu(mockEvent)
    })

    expect(preventDefault).toHaveBeenCalled()
    expect(mocks.navigate).toHaveBeenCalledWith('route/my-id')
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
