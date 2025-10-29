import { describe, expect, it } from 'vitest'
import type { Node } from '@xyflow/react'
import type { Adapter, Bridge, EntityReference, ProtocolAdapter } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types'
import type { NodeCombinerType, NodePulseType } from '@/modules/Workspace/types'
import {
  buildEntityReferencesFromNodes,
  filterCombinerCandidates,
  findExistingCombiner,
  isAssetMapperCombiner,
  isNodeCombinerCandidate,
  type CombinerEligibleNode,
} from './toolbar.utils'

describe('toolbar.utils', () => {
  describe('isNodeCombinerCandidate', () => {
    it.each([
      {
        description: 'adapter nodes with COMBINE capability',
        adapterType: 'mqtt',
        capabilities: ['COMBINE', 'READ'],
        adapterTypes: [{ id: 'mqtt', capabilities: ['COMBINE', 'READ'] } as ProtocolAdapter],
        expected: true,
      },
      {
        description: 'adapter nodes without COMBINE capability',
        adapterType: 'modbus',
        capabilities: ['READ'],
        adapterTypes: [{ id: 'modbus', capabilities: ['READ'] } as ProtocolAdapter],
        expected: false,
      },
      {
        description: 'adapter nodes when adapter type is not found',
        adapterType: 'unknown',
        capabilities: [],
        adapterTypes: [{ id: 'mqtt', capabilities: ['COMBINE'] } as ProtocolAdapter],
        expected: false,
      },
      {
        description: 'adapter nodes when adapter types are undefined',
        adapterType: 'mqtt',
        capabilities: [],
        adapterTypes: undefined,
        expected: false,
      },
    ])('should return $expected for $description', ({ adapterType, adapterTypes, expected }) => {
      const adapterNode: Node<Adapter, NodeTypes.ADAPTER_NODE> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          type: adapterType,
        } as Adapter,
      }

      expect(isNodeCombinerCandidate(adapterNode, adapterTypes)).toBe(expected)
    })

    it.each([
      {
        description: 'bridge nodes',
        node: {
          id: 'bridge-1',
          type: NodeTypes.BRIDGE_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'bridge-1' } as Bridge,
        } as Node<Bridge, NodeTypes.BRIDGE_NODE>,
        expected: true,
      },
      {
        description: 'pulse nodes',
        node: {
          id: 'pulse-1',
          type: NodeTypes.PULSE_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'pulse-1', label: 'Pulse Agent' },
        } as NodePulseType,
        expected: true,
      },
      {
        description: 'other node types',
        node: {
          id: 'edge-1',
          type: NodeTypes.EDGE_NODE,
          position: { x: 0, y: 0 },
          data: { label: 'Edge' },
        } as Node,
        expected: false,
      },
    ])('should return $expected for $description', ({ node, expected }) => {
      expect(isNodeCombinerCandidate(node)).toBe(expected)
    })
  })

  describe('buildEntityReferencesFromNodes', () => {
    it.each([
      {
        description: 'adapter nodes',
        nodes: [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1', type: 'mqtt' } as Adapter,
          },
        ] as CombinerEligibleNode[],
        expected: [
          { type: EntityType.ADAPTER, id: 'adapter-1' },
          { id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER },
        ],
      },
      {
        description: 'bridge nodes',
        nodes: [
          {
            id: 'bridge-1',
            type: NodeTypes.BRIDGE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'bridge-1' } as Bridge,
          },
        ] as CombinerEligibleNode[],
        expected: [
          { type: EntityType.BRIDGE, id: 'bridge-1' },
          { id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER },
        ],
      },
      {
        description: 'pulse nodes',
        nodes: [
          {
            id: 'pulse-1',
            type: NodeTypes.PULSE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'pulse-1', label: 'Pulse' },
          },
        ] as CombinerEligibleNode[],
        expected: [
          { type: EntityType.PULSE_AGENT, id: 'pulse-1' },
          { id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER },
        ],
      },
    ])('should build entity references from $description', ({ nodes, expected }) => {
      const result = buildEntityReferencesFromNodes(nodes)
      expect(result).toEqual(expected)
    })

    it('should build entity references from mixed node types', () => {
      const nodes: CombinerEligibleNode[] = [
        {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'adapter-1', type: 'mqtt' } as Adapter,
        },
        {
          id: 'bridge-1',
          type: NodeTypes.BRIDGE_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'bridge-1' } as Bridge,
        },
        {
          id: 'pulse-1',
          type: NodeTypes.PULSE_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'pulse-1', label: 'Pulse' },
        },
      ]

      const result = buildEntityReferencesFromNodes(nodes)

      expect(result).toEqual([
        { type: EntityType.ADAPTER, id: 'adapter-1' },
        { type: EntityType.BRIDGE, id: 'bridge-1' },
        { type: EntityType.PULSE_AGENT, id: 'pulse-1' },
        { id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER },
      ])
    })

    it('should always include edge broker as last reference', () => {
      const nodes: CombinerEligibleNode[] = []
      const result = buildEntityReferencesFromNodes(nodes)

      expect(result).toEqual([{ id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER }])
    })
  })

  describe('findExistingCombiner', () => {
    it('should find combiner with exact same sources', () => {
      const targetReferences: EntityReference[] = [
        { type: EntityType.ADAPTER, id: 'adapter-1' },
        { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
      ]

      const combinerNode: NodeCombinerType = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'combiner-1',
          name: 'Test Combiner',
          sources: {
            items: [
              { type: EntityType.ADAPTER, id: 'adapter-1' },
              { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
            ],
          },
          mappings: { items: [] },
        },
      }

      const allNodes: Node[] = [combinerNode]

      const result = findExistingCombiner(allNodes, targetReferences)

      expect(result).toEqual(combinerNode)
    })

    it('should find combiner with sources in different order', () => {
      const targetReferences: EntityReference[] = [
        { type: EntityType.ADAPTER, id: 'adapter-1' },
        { type: EntityType.BRIDGE, id: 'bridge-1' },
        { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
      ]

      const combinerNode: NodeCombinerType = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'combiner-1',
          name: 'Test Combiner',
          sources: {
            items: [
              { type: EntityType.BRIDGE, id: 'bridge-1' },
              { type: EntityType.ADAPTER, id: 'adapter-1' },
              { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
            ],
          },
          mappings: { items: [] },
        },
      }

      const allNodes: Node[] = [combinerNode]

      const result = findExistingCombiner(allNodes, targetReferences)

      expect(result).toEqual(combinerNode)
    })

    it.each([
      {
        description: 'no matching combiner exists',
        targetReferences: [
          { type: EntityType.ADAPTER, id: 'adapter-1' },
          { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
        ] as EntityReference[],
        combinerNode: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          position: { x: 0, y: 0 },
          data: {
            id: 'combiner-1',
            name: 'Test Combiner',
            sources: {
              items: [
                { type: EntityType.ADAPTER, id: 'adapter-2' },
                { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
              ],
            },
            mappings: { items: [] },
          },
        } as NodeCombinerType,
      },
      {
        description: 'sources have different lengths',
        targetReferences: [
          { type: EntityType.ADAPTER, id: 'adapter-1' },
          { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
        ] as EntityReference[],
        combinerNode: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          position: { x: 0, y: 0 },
          data: {
            id: 'combiner-1',
            name: 'Test Combiner',
            sources: {
              items: [
                { type: EntityType.ADAPTER, id: 'adapter-1' },
                { type: EntityType.BRIDGE, id: 'bridge-1' },
                { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
              ],
            },
            mappings: { items: [] },
          },
        } as NodeCombinerType,
      },
    ])('should return undefined when $description', ({ targetReferences, combinerNode }) => {
      const allNodes: Node[] = [combinerNode]
      const result = findExistingCombiner(allNodes, targetReferences)

      expect(result).toBeUndefined()
    })

    it('should return undefined when no combiner nodes exist', () => {
      const targetReferences: EntityReference[] = [
        { type: EntityType.ADAPTER, id: 'adapter-1' },
        { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
      ]

      const adapterNode: Node<Adapter, NodeTypes.ADAPTER_NODE> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: { id: 'adapter-1' } as Adapter,
      }

      const allNodes: Node[] = [adapterNode]

      const result = findExistingCombiner(allNodes, targetReferences)

      expect(result).toBeUndefined()
    })

    it('should return first matching combiner when multiple exist', () => {
      const targetReferences: EntityReference[] = [
        { type: EntityType.ADAPTER, id: 'adapter-1' },
        { type: EntityType.EDGE_BROKER, id: IdStubs.EDGE_NODE },
      ]

      const combiner1: NodeCombinerType = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'combiner-1',
          name: 'First Combiner',
          sources: {
            items: targetReferences,
          },
          mappings: { items: [] },
        },
      }

      const combiner2: NodeCombinerType = {
        id: 'combiner-2',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'combiner-2',
          name: 'Second Combiner',
          sources: {
            items: targetReferences,
          },
          mappings: { items: [] },
        },
      }

      const allNodes: Node[] = [combiner1, combiner2]

      const result = findExistingCombiner(allNodes, targetReferences)

      expect(result).toEqual(combiner1)
    })
  })

  describe('filterCombinerCandidates', () => {
    const adapterTypes: ProtocolAdapter[] = [
      {
        id: 'mqtt',
        capabilities: ['COMBINE'],
      } as ProtocolAdapter,
      {
        id: 'modbus',
        capabilities: ['READ'],
      } as ProtocolAdapter,
    ]

    it.each([
      {
        description: 'eligible adapter nodes',
        nodes: [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1', type: 'mqtt' } as Adapter,
          },
        ] as Node[],
        expectedLength: 1,
        expectedIds: ['adapter-1'],
      },
      {
        description: 'bridge nodes',
        nodes: [
          {
            id: 'bridge-1',
            type: NodeTypes.BRIDGE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'bridge-1' } as Bridge,
          },
        ] as Node[],
        expectedLength: 1,
        expectedIds: ['bridge-1'],
      },
      {
        description: 'pulse nodes',
        nodes: [
          {
            id: 'pulse-1',
            type: NodeTypes.PULSE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'pulse-1', label: 'Pulse' },
          },
        ] as Node[],
        expectedLength: 1,
        expectedIds: ['pulse-1'],
      },
    ])('should return $description', ({ nodes, expectedLength, expectedIds }) => {
      const result = filterCombinerCandidates(nodes, adapterTypes)

      expect(result).toHaveLength(expectedLength)
      expect(result?.map((n) => n.id)).toEqual(expectedIds)
    })

    it.each([
      {
        description: 'ineligible adapter nodes',
        nodes: [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1', type: 'modbus' } as Adapter,
          },
        ] as Node[],
      },
      {
        description: 'no eligible nodes exist',
        nodes: [
          {
            id: 'edge-1',
            type: NodeTypes.EDGE_NODE,
            position: { x: 0, y: 0 },
            data: { label: 'Edge' },
          },
        ] as Node[],
      },
      {
        description: 'empty array',
        nodes: [] as Node[],
      },
    ])('should return undefined when $description', ({ nodes }) => {
      const result = filterCombinerCandidates(nodes, adapterTypes)
      expect(result).toBeUndefined()
    })

    it('should filter mixed node types correctly', () => {
      const nodes: Node[] = [
        {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'adapter-1', type: 'mqtt' } as Adapter,
        },
        {
          id: 'adapter-2',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'adapter-2', type: 'modbus' } as Adapter,
        },
        {
          id: 'bridge-1',
          type: NodeTypes.BRIDGE_NODE,
          position: { x: 0, y: 0 },
          data: { id: 'bridge-1' } as Bridge,
        },
        {
          id: 'edge-1',
          type: NodeTypes.EDGE_NODE,
          position: { x: 0, y: 0 },
          data: { label: 'Edge' },
        },
      ]

      const result = filterCombinerCandidates(nodes, adapterTypes)

      expect(result).toHaveLength(2)
      expect(result?.map((n) => n.id)).toEqual(['adapter-1', 'bridge-1'])
    })
  })

  describe('isAssetMapperCombiner', () => {
    it.each([
      {
        description: 'nodes contain a pulse node',
        nodes: [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' } as Adapter,
          },
          {
            id: 'pulse-1',
            type: NodeTypes.PULSE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'pulse-1', label: 'Pulse' },
          },
        ] as CombinerEligibleNode[],
        expected: true,
      },
      {
        description: 'nodes do not contain a pulse node',
        nodes: [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' } as Adapter,
          },
          {
            id: 'bridge-1',
            type: NodeTypes.BRIDGE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'bridge-1' } as Bridge,
          },
        ] as CombinerEligibleNode[],
        expected: false,
      },
      {
        description: 'nodes are undefined',
        nodes: undefined,
        expected: false,
      },
      {
        description: 'nodes are an empty array',
        nodes: [] as CombinerEligibleNode[],
        expected: false,
      },
    ])('should return $expected when $description', ({ nodes, expected }) => {
      expect(isAssetMapperCombiner(nodes)).toBe(expected)
    })
  })
})
