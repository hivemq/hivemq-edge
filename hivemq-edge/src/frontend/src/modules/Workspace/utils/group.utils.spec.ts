import { expect } from 'vitest'
import { getGroupLayout } from './group.utils.ts'
import { Rect } from 'reactflow'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'

describe('getGroupLayout', () => {
  it('should return the layout characteristics of a group', async () => {
    expect(
      getGroupLayout([{ ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 }, width: 50, height: 100 }])
    ).toStrictEqual<Rect>({
      height: 164,
      width: 90,
      x: -20,
      y: -44,
    })
  })
})
