import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'

import { getAccessibleDraggableProvider } from '@/__test-utils__/hooks/WrapperAccessibleDraggableProvider.tsx'

import type { AccessibleDraggableProps } from '@/hooks/useAccessibleDraggable/type.ts'
import { useAccessibleDraggable } from '@/hooks/useAccessibleDraggable/useAccessibleDraggable.ts'

describe('useAccessibleDraggable', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('should be used in the right context', () => {
    expect(() => {
      renderHook(() => useAccessibleDraggable())
    }).toThrow('useAccessibleDraggable must be used within AccessibleDraggableContext')
  })

  it('should return the canvas options', () => {
    const { result } = renderHook(() => useAccessibleDraggable(), { wrapper: getAccessibleDraggableProvider() })
    expect(result.current).toStrictEqual<AccessibleDraggableProps>(
      expect.objectContaining({
        isDragging: false,
        ref: {
          current: null,
        },
        source: undefined,
      })
    )
  })
})
