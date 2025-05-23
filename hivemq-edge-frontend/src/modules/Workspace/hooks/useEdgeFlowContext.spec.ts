import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'

import { useEdgeFlowContext } from './useEdgeFlowContext.ts'
import { getWrapperEdgeProvider } from '@/__test-utils__/hooks/WrapperEdgeProvider.tsx'
import type { EdgeFlowOptions } from '@/modules/Workspace/types.ts'

describe('useEdgeFlowContext', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('should be used in the right context', () => {
    expect(() => {
      renderHook(() => useEdgeFlowContext())
    }).toThrow('useEdgeFlowContext must be used within a EdgeFlowContext')
  })

  it('should return the canvas options', () => {
    const { result } = renderHook(() => useEdgeFlowContext(), { wrapper: getWrapperEdgeProvider() })
    expect(result.current.options).toEqual<EdgeFlowOptions>({
      showGateway: false,
      showTopics: true,
      showStatus: true,
    })
  })
})
