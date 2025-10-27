import { describe, it, expect } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'
import { EntityType, PulseStatus } from '@/api/__generated__'
import { NodeTypes } from '@/modules/Workspace/types'
import { updateEdgesStatusWithModel } from '@/modules/Workspace/utils/status-utils'
import { RuntimeStatus, OperationalStatus } from '@/modules/Workspace/types/status.types'

const MOCK_THEME = {
  colors: {
    status: {
      connected: { 500: '#00FF00' },
      error: { 500: '#FF0000' },
      disconnected: { 500: '#FFFF00' },
    },
    brand: { 500: '#0000FF' },
  },
}

describe('Pulse to Asset Mapper edge status', () => {
  it('should use asset mapper operational status for edge animation', () => {
    // Pulse node with ACTIVE operational status (has connected mappers with valid mappings)
    const pulseNode: Node = {
      id: 'pulse-1',
      type: NodeTypes.PULSE_NODE,
      data: {
        id: 'pulse-1',
        label: 'Pulse Agent',
        status: {
          activation: PulseStatus.activation.ACTIVATED,
          runtime: PulseStatus.runtime.CONNECTED,
        },
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE, // Overall Pulse is operational
          source: 'PULSE' as const,
        },
      },
      position: { x: 0, y: 0 },
    }

    // Asset mapper WITHOUT mappings (operational INACTIVE)
    const assetMapperNoMappings: Node = {
      id: 'mapper-1',
      type: NodeTypes.COMBINER_NODE,
      data: {
        id: 'mapper-1',
        name: 'Asset Mapper 1',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT, name: 'Pulse' }],
        },
        mappings: { items: [] }, // NO MAPPINGS!
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.INACTIVE, // No mappings!
          source: 'DERIVED' as const,
        },
      },
      position: { x: 100, y: 0 },
    }

    // Asset mapper WITH valid mappings (operational ACTIVE)
    const assetMapperWithMappings: Node = {
      id: 'mapper-2',
      type: NodeTypes.COMBINER_NODE,
      data: {
        id: 'mapper-2',
        name: 'Asset Mapper 2',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT, name: 'Pulse' }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { entityId: 'pulse-1', dataIdentifier: 'tag1' } },
              destination: { assetId: 'asset-1', topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE, // Has mappings!
          source: 'DERIVED' as const,
        },
      },
      position: { x: 200, y: 0 },
    }

    const edges: Edge[] = [
      {
        id: 'edge-1',
        source: 'pulse-1',
        target: 'mapper-1',
        markerEnd: { type: MarkerType.ArrowClosed },
      },
      {
        id: 'edge-2',
        source: 'pulse-1',
        target: 'mapper-2',
        markerEnd: { type: MarkerType.ArrowClosed },
      },
    ]

    const getNode = (id: string) => {
      const nodes = [pulseNode, assetMapperNoMappings, assetMapperWithMappings]
      return nodes.find((n) => n.id === id)
    }

    const result = updateEdgesStatusWithModel([], edges, getNode, MOCK_THEME)

    // Edge to mapper WITHOUT mappings should NOT be animated
    const edge1 = result.find((e) => e.id === 'edge-1')
    expect(edge1).toBeDefined()
    expect(edge1?.animated).toBe(false) // No animation because operational = INACTIVE

    // Edge to mapper WITH mappings SHOULD be animated
    const edge2 = result.find((e) => e.id === 'edge-2')
    expect(edge2).toBeDefined()
    expect(edge2?.animated).toBe(true) // Animated because operational = ACTIVE
  })

  it('should handle non-asset-mapper combiners normally', () => {
    const pulseNode: Node = {
      id: 'pulse-1',
      type: NodeTypes.PULSE_NODE,
      data: {
        id: 'pulse-1',
        label: 'Pulse Agent',
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'PULSE' as const,
        },
      },
      position: { x: 0, y: 0 },
    }

    // Regular combiner (not an asset mapper - has adapter source, not pulse)
    const regularCombiner: Node = {
      id: 'combiner-1',
      type: NodeTypes.COMBINER_NODE,
      data: {
        id: 'combiner-1',
        name: 'Regular Combiner',
        sources: {
          items: [{ id: 'adapter-1', type: EntityType.ADAPTER, name: 'Adapter' }],
        },
        mappings: { items: [] },
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.INACTIVE,
          source: 'DERIVED' as const,
        },
      },
      position: { x: 100, y: 0 },
    }

    const edges: Edge[] = [
      {
        id: 'edge-1',
        source: 'pulse-1',
        target: 'combiner-1',
        markerEnd: { type: MarkerType.ArrowClosed },
      },
    ]

    const getNode = (id: string) => {
      return [pulseNode, regularCombiner].find((n) => n.id === id)
    }

    const result = updateEdgesStatusWithModel([], edges, getNode, MOCK_THEME)

    // Edge to non-asset-mapper should use Pulse node's overall status
    const edge1 = result.find((e) => e.id === 'edge-1')
    expect(edge1).toBeDefined()
    // Should use Pulse node's operational status (ACTIVE)
    expect(edge1?.animated).toBe(true)
  })
})
