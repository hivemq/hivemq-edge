import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'

import { useEdgeFlowContext } from './useEdgeFlowContext.tsx'
import { EdgeFlowProvider } from './FlowContext.tsx'
import { EdgeFlowOptions } from '@/modules/Workspace/types.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <EdgeFlowProvider>{children}</EdgeFlowProvider>
)

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
    const { result } = renderHook(() => useEdgeFlowContext(), { wrapper })
    expect(result.current.options).toEqual<EdgeFlowOptions>({
      showGateway: false,
      showHosts: false,
      showMonitoringOnEdge: true,
      showTopics: true,
      showStatus: true,
    })
  })
})
