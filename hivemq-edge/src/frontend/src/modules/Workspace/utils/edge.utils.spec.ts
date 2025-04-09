import { expect } from 'vitest'
import type { InternalNode, Node } from '@xyflow/react'
import { Position } from '@xyflow/react'
import { MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes'

import { getEdgeParams, getEdgePosition, getNodeIntersection } from './edge.utils'

type MeasuredType = {
  width?: number
  height?: number
}

const makeInternalNode = <T extends Node>(node: T, w: MeasuredType): InternalNode<T> => ({
  ...node,
  measured: w,
  internals: {
    positionAbsolute: node.position,
    z: 0,
    userNode: node,
  },
})

describe('getNodeIntersection', () => {
  const nodes2 = makeInternalNode(
    { ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } },
    {
      width: 100,
      height: 100,
    }
  )
  it('should fail if at the same location', async () => {
    const nodes1 = makeInternalNode(
      { ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } },
      {
        width: 100,
        height: 100,
      }
    )

    expect(getNodeIntersection(nodes1, nodes2)).toStrictEqual({
      x: NaN,
      y: NaN,
    })
  })

  it('should be on the boundary', async () => {
    const nodes1 = makeInternalNode(
      { ...MOCK_NODE_EDGE, position: { x: 250, y: 100 } },
      {
        width: 100,
        height: 100,
      }
    )

    expect(getNodeIntersection(nodes1, nodes2)).toStrictEqual({
      x: 250,
      y: 130,
    })
  })
})

describe('getEdgePosition', () => {
  const nodes1 = makeInternalNode(
    { ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } },
    {
      width: 50,
      height: 50,
    }
  )

  it('should be on the left when in the middle', async () => {
    expect(getEdgePosition(nodes1, { x: 0, y: 0 })).toStrictEqual(Position.Left)
  })

  it('should be on the left ', async () => {
    expect(getEdgePosition(nodes1, { x: -100, y: 0 })).toStrictEqual(Position.Left)
  })

  it('should be on the right ', async () => {
    expect(getEdgePosition(nodes1, { x: 100, y: 0 })).toStrictEqual(Position.Right)
  })

  it('should be on the top ', async () => {
    expect(getEdgePosition(nodes1, { x: 2, y: -100 })).toStrictEqual(Position.Top)
  })

  it('should be on the bottom ', async () => {
    expect(getEdgePosition(nodes1, { x: 2, y: 100 })).toStrictEqual(Position.Bottom)
  })
})

describe('getEdgeParams', () => {
  const nodes1 = makeInternalNode(
    { ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } },
    {
      width: 50,
      height: 50,
    }
  )

  const nodes2 = makeInternalNode(
    { ...MOCK_NODE_EDGE, position: { x: 100, y: 100 } },
    {
      width: 50,
      height: 50,
    }
  )

  it('should be on the left when in the middle', async () => {
    expect(getEdgeParams(nodes1, nodes2)).toStrictEqual({
      sourcePos: 'right',
      sx: 50,
      sy: 50,
      targetPos: 'left',
      tx: 100,
      ty: 100,
    })
  })
})
