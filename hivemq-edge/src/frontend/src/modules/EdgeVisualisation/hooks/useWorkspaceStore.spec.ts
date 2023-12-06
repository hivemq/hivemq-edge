import { expect } from 'vitest'
import { renderHook } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { handlers } from '@/api/hooks/useEvents/__handlers__'

import { RFState } from '../types.ts'
import useWorkspaceStore from './useWorkspaceStore.ts'

describe('useWorkspaceStore', () => {
  it('should start with an empty store', async () => {
    server.use(...handlers)

    const { result } = renderHook<RFState, unknown>(useWorkspaceStore)
    const { nodes, edges } = result.current

    expect(nodes).toHaveLength(0)
    expect(edges).toHaveLength(0)
  })
})
