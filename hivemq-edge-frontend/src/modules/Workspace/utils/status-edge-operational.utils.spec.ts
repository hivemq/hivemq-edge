import { describe, it, expect } from 'vitest'
import type { Combiner, ManagedAsset } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { AssetMapping, EntityType } from '@/api/__generated__'
import type { Node } from '@xyflow/react'
import { NodeTypes, type NodeCombinerType } from '../types'
import {
  combinerHasValidPulseAssetMappings,
  computePulseToAssetMapperOperationalStatus,
  computePulseNodeOperationalStatus,
} from './status-edge-operational.utils.ts'
import { OperationalStatus } from '../types/status.types'

describe('edge-operational-status.utils', () => {
  describe('combinerHasValidPulseAssetMappings', () => {
    const mockMappedAssets: ManagedAsset[] = [
      {
        id: 'asset-1',
        topic: 'test/asset1',
        mapping: { status: AssetMapping.status.STREAMING },
      } as ManagedAsset,
      {
        id: 'asset-2',
        topic: 'test/asset2',
        mapping: { status: AssetMapping.status.STREAMING },
      } as ManagedAsset,
    ]

    const mockUnmappedAssets: ManagedAsset[] = [
      {
        id: 'asset-3',
        topic: 'test/asset3',
        mapping: { status: AssetMapping.status.UNMAPPED },
      } as ManagedAsset,
    ]

    it('should return false if combiner has no mappings', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Test Combiner',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
        },
        mappings: { items: [] },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockMappedAssets)).toBe(false)
    })

    it('should return false if combiner has no Pulse source', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Test Combiner',
        sources: {
          items: [{ id: 'adapter-1', type: EntityType.ADAPTER }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { assetId: 'asset-1', topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockMappedAssets)).toBe(false)
    })

    it('should return true if combiner has valid mapped asset reference', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Asset Mapper',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { assetId: 'asset-1', topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockMappedAssets)).toBe(true)
    })

    it('should return false if mapping references unmapped asset', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Asset Mapper',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { assetId: 'asset-3', topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockUnmappedAssets)).toBe(false)
    })

    it('should return false if mapping references non-existent asset', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Asset Mapper',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { assetId: 'non-existent-asset', topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockMappedAssets)).toBe(false)
    })

    it('should return false if mapping has no assetId', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Asset Mapper',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockMappedAssets)).toBe(false)
    })

    it('should return true if at least one mapping is valid (mixed scenario)', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Asset Mapper',
        sources: {
          items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { assetId: 'non-existent', topic: 'test/topic1' },
              instructions: [],
            },
            {
              id: 'mapping-2',
              sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
              destination: { assetId: 'asset-1', topic: 'test/topic2' },
              instructions: [],
            },
          ],
        },
      }

      expect(combinerHasValidPulseAssetMappings(combiner, mockMappedAssets)).toBe(true)
    })
  })

  describe('computePulseToAssetMapperOperationalStatus', () => {
    const mockMappedAssets: ManagedAsset[] = [
      {
        id: 'asset-1',
        topic: 'test/asset1',
        mapping: { status: AssetMapping.status.STREAMING },
      } as ManagedAsset,
    ]

    it('should return ERROR if source is not a Pulse node', () => {
      const source: Node = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        data: {},
        position: { x: 0, y: 0 },
      }

      const target: Node = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        data: {
          id: 'combiner-1',
          name: 'Combiner',
          sources: { items: [] },
          mappings: { items: [] },
        },
        position: { x: 0, y: 0 },
      }

      expect(computePulseToAssetMapperOperationalStatus(source, target, mockMappedAssets)).toBe(OperationalStatus.ERROR)
    })

    it('should return ERROR if target is not a Combiner node', () => {
      const source: Node = {
        id: 'pulse-1',
        type: NodeTypes.PULSE_NODE,
        data: { id: 'pulse-1', label: 'Pulse' },
        position: { x: 0, y: 0 },
      }

      const target: Node = {
        id: 'edge-1',
        type: NodeTypes.EDGE_NODE,
        data: { label: 'Edge' },
        position: { x: 0, y: 0 },
      }

      expect(computePulseToAssetMapperOperationalStatus(source, target, mockMappedAssets)).toBe(OperationalStatus.ERROR)
    })

    it('should return ERROR if combiner is not an asset mapper (no Pulse source)', () => {
      const source: Node = {
        id: 'pulse-1',
        type: NodeTypes.PULSE_NODE,
        data: { id: 'pulse-1', label: 'Pulse' },
        position: { x: 0, y: 0 },
      }

      const target: Node = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        data: {
          id: 'combiner-1',
          name: 'Regular Combiner',
          sources: {
            items: [{ id: 'adapter-1', type: EntityType.ADAPTER, name: 'Adapter' }],
          },
          mappings: { items: [] },
        },
        position: { x: 0, y: 0 },
      }

      expect(computePulseToAssetMapperOperationalStatus(source, target, mockMappedAssets)).toBe(OperationalStatus.ERROR)
    })

    it('should return INACTIVE if asset mapper has no valid mappings', () => {
      const source: Node = {
        id: 'pulse-1',
        type: NodeTypes.PULSE_NODE,
        data: { id: 'pulse-1', label: 'Pulse' },
        position: { x: 0, y: 0 },
      }

      const target: Node = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        data: {
          id: 'combiner-1',
          name: 'Asset Mapper',
          sources: {
            items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
          },
          mappings: { items: [] },
        },
        position: { x: 0, y: 0 },
      }

      expect(computePulseToAssetMapperOperationalStatus(source, target, mockMappedAssets)).toBe(
        OperationalStatus.INACTIVE
      )
    })

    it('should return ACTIVE if asset mapper has valid mapped asset reference', () => {
      const source: Node = {
        id: 'pulse-1',
        type: NodeTypes.PULSE_NODE,
        data: { id: 'pulse-1', label: 'Pulse' },
        position: { x: 0, y: 0 },
      }

      const target: Node = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        data: {
          id: 'combiner-1',
          name: 'Asset Mapper',
          sources: {
            items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
          },
          mappings: {
            items: [
              {
                id: 'mapping-1',
                sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
                destination: { assetId: 'asset-1', topic: 'test/topic' },
                instructions: [],
              },
            ],
          },
        },
        position: { x: 0, y: 0 },
      }

      expect(computePulseToAssetMapperOperationalStatus(source, target, mockMappedAssets)).toBe(
        OperationalStatus.ACTIVE
      )
    })
  })

  describe('computePulseNodeOperationalStatus', () => {
    const mockMappedAssets: ManagedAsset[] = [
      {
        id: 'asset-1',
        topic: 'test/asset1',
        mapping: { status: AssetMapping.status.STREAMING },
      } as ManagedAsset,
    ]

    it('should return INACTIVE if Pulse node has no connected combiners', () => {
      expect(computePulseNodeOperationalStatus([], mockMappedAssets)).toBe(OperationalStatus.INACTIVE)
    })

    it('should return INACTIVE if no connected combiner has valid mappings', () => {
      const combiners: NodeCombinerType[] = [
        {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Asset Mapper 1',
            sources: {
              items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
            },
            mappings: { items: [] },
          },
          position: { x: 0, y: 0 },
        },
      ]

      expect(computePulseNodeOperationalStatus(combiners, mockMappedAssets)).toBe(OperationalStatus.INACTIVE)
    })

    it('should return ACTIVE if at least one combiner has valid mappings', () => {
      const combiners: NodeCombinerType[] = [
        {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Asset Mapper 1',
            sources: {
              items: [{ id: 'pulse-1', type: EntityType.PULSE_AGENT }],
            },
            mappings: { items: [] },
          },
          position: { x: 0, y: 0 },
        },
        {
          id: 'combiner-2',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-2',
            name: 'Asset Mapper 2',
            sources: {
              items: [
                { id: 'test1', type: EntityType.ADAPTER },
                { id: 'pulse-1', type: EntityType.PULSE_AGENT },
              ],
            },
            mappings: {
              items: [
                {
                  id: 'mapping-1',
                  sources: { primary: { id: 'pulse-1', type: DataIdentifierReference.type.TAG, scope: 'pulse-1' } },
                  destination: { assetId: 'asset-1', topic: 'test/topic' },
                  instructions: [],
                },
              ],
            },
          },
          position: { x: 0, y: 0 },
        },
      ]

      expect(computePulseNodeOperationalStatus(combiners, mockMappedAssets)).toBe(OperationalStatus.ACTIVE)
    })
  })
})
