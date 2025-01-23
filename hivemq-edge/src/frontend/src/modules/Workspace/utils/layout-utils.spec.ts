import { expect } from 'vitest'
import type { Node } from 'reactflow'

import type { EdgeFlowGrouping } from '../types.ts'
import { EdgeFlowLayout } from '../types.ts'

import { MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { applyLayout } from '@/modules/Workspace/utils/layout-utils.ts'

describe('applyLayout', () => {
  const defaultEdgeFlowGrouping: EdgeFlowGrouping = {
    keys: [],
    showGroups: false,
    layout: EdgeFlowLayout.HORIZONTAL,
  }

  it('should apply the default layout', async () => {
    const nodes: Node[] = [{ ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } }]
    const layoutNodes = applyLayout(nodes, defaultEdgeFlowGrouping)
    for (const mapElement of layoutNodes) {
      const origin = nodes.find((node) => node.id === mapElement.id)
      expect(origin).not.toBeUndefined()
      expect(mapElement.position).toStrictEqual(origin?.position)
    }
  })
})
