import { expect } from 'vitest'
import type { Rect } from '@xyflow/react'
import { getGroupBounds } from './group.utils.ts'

describe('getGroupBounds', () => {
  it('should return the layout characteristics of a group', async () => {
    expect(getGroupBounds({ x: 0, y: 0, width: 50, height: 100 })).toStrictEqual<Rect>({
      height: 164,
      width: 90,
      x: -20,
      y: -44,
    })
  })
})
