import { describe, expect } from 'vitest'
import { initialStore } from './store.utils.ts'
import { WorkspaceState } from '@/extensions/datahub/types.ts'

describe('initialStore', () => {
  it('should return the initial state of the store', async () => {
    expect(initialStore()).toStrictEqual<WorkspaceState>({
      nodes: [],
      edges: [],
      functions: expect.arrayContaining([]),
    })
  })
})
