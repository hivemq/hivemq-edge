import { expect } from 'vitest'
import { isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('isBidirectional', () => {
  it('should return the layout characteristics of a group', async () => {
    expect(isBidirectional(mockProtocolAdapter)).toStrictEqual(false)
    expect(isBidirectional({ ...mockProtocolAdapter, id: 'opc-ua-client' })).toStrictEqual(true)
  })
})
