import { expect } from 'vitest'
import type { Edge, Rect } from '@xyflow/react'
import { createGroup, getGroupBounds } from './group.utils.ts'
import { MOCK_THEME } from '../../../__test-utils__/react-flow/utils'
import { MOCK_NODE_ADAPTER } from '../../../__test-utils__/react-flow/nodes'
import type { NodeGroupType } from '../types'
import { EdgeTypes, NodeTypes } from '../types'

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

describe('createGroup', () => {
  it('should create a group out of adapters', async () => {
    const test = createGroup(
      [{ ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }],
      { x: 0, y: 0, width: 50, height: 100 },
      MOCK_THEME
    )
    expect(test.newGroupNode).toStrictEqual<NodeGroupType>(
      expect.objectContaining({
        type: NodeTypes.CLUSTER_NODE,
        id: 'group@my-adapter',
        data: {
          childrenNodeIds: ['idAdapter'],
          colorScheme: 'red',
          isOpen: true,
          title: 'Untitled group',
        },
        position: {
          x: 0,
          y: 0,
        },
        style: {
          height: 100,
          width: 50,
        },
      })
    )
    expect(test.newGroupEdge).toStrictEqual<Edge>(
      expect.objectContaining({
        id: 'connect-edge-group@my-adapter',
        source: 'group@my-adapter',
        target: 'edge',
        targetHandle: 'Top',
        type: EdgeTypes.DYNAMIC_EDGE,
      })
    )
  })
})
