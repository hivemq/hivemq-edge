/**
 * Test to verify constraint extraction for ADAPTER+DEVICE nodes
 */

import { describe, it, expect } from 'vitest'
import { extractLayoutConstraints } from './constraint-utils'
import { NodeTypes } from '../../types'
import type { Node, Edge } from '@xyflow/react'

describe('extractLayoutConstraints - ADAPTER/DEVICE gluing', () => {
  it('should correctly identify DEVICE as glued child of ADAPTER', () => {
    const nodes: Node[] = [
      { id: 'edge-1', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
      { id: 'adapter-1', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: { id: 'adapter-1' } },
      { id: 'device-1', type: NodeTypes.DEVICE_NODE, position: { x: 0, y: 0 }, data: { sourceAdapterId: 'adapter-1' } },
      { id: 'adapter-2', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: { id: 'adapter-2' } },
      { id: 'device-2', type: NodeTypes.DEVICE_NODE, position: { x: 0, y: 0 }, data: { sourceAdapterId: 'adapter-2' } },
    ]

    const edges: Edge[] = []

    const constraints = extractLayoutConstraints(nodes, edges)

    console.log('Extracted glued nodes:', Array.from(constraints.gluedNodes.entries()))

    // DEVICE nodes should be glued to ADAPTER nodes
    expect(constraints.gluedNodes.has('device-1')).toBe(true)
    expect(constraints.gluedNodes.has('device-2')).toBe(true)

    // ADAPTER nodes should NOT be glued (they are parents)
    expect(constraints.gluedNodes.has('adapter-1')).toBe(false)
    expect(constraints.gluedNodes.has('adapter-2')).toBe(false)

    // Check the parent relationships - should match by sourceAdapterId
    const device1Info = constraints.gluedNodes.get('device-1')
    expect(device1Info?.parentId).toBe('adapter-1')

    const device2Info = constraints.gluedNodes.get('device-2')
    expect(device2Info?.parentId).toBe('adapter-2') // Now correctly maps to adapter-2!
  })

  it('should handle multiple DEVICE nodes with sourceAdapterId matching', () => {
    // With sourceAdapterId, each DEVICE should match to its specific ADAPTER
    const nodes: Node[] = [
      { id: 'adapter-1', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: { id: 'adapter-1' } },
      { id: 'adapter-2', type: NodeTypes.ADAPTER_NODE, position: { x: 100, y: 0 }, data: { id: 'adapter-2' } },
      {
        id: 'device-1',
        type: NodeTypes.DEVICE_NODE,
        position: { x: 0, y: 100 },
        data: { sourceAdapterId: 'adapter-1' },
      },
      {
        id: 'device-2',
        type: NodeTypes.DEVICE_NODE,
        position: { x: 100, y: 100 },
        data: { sourceAdapterId: 'adapter-2' },
      },
    ]

    const edges: Edge[] = []

    const constraints = extractLayoutConstraints(nodes, edges)

    console.log('Multiple ADAPTER/DEVICE pairs:', Array.from(constraints.gluedNodes.entries()))

    // Now correctly matches by sourceAdapterId!
    const device1Info = constraints.gluedNodes.get('device-1')
    const device2Info = constraints.gluedNodes.get('device-2')

    console.log('device-1 parent:', device1Info?.parentId)
    console.log('device-2 parent:', device2Info?.parentId)

    // Each device now points to its correct adapter
    expect(device1Info?.parentId).toBe('adapter-1')
    expect(device2Info?.parentId).toBe('adapter-2') // ✅ Fixed!
  })
})
