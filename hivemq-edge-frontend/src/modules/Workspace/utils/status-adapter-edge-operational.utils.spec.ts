/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from 'vitest'
import type { AdapterConfig, Combiner } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import type { Node } from '@xyflow/react'
import { NodeTypes } from '../types'
import {
  adapterHasNorthboundMappings,
  adapterHasSouthboundMappings,
  getDeviceTagNames,
  combinerHasValidAdapterTagMappings,
  computeAdapterToCombinerOperationalStatus,
  computeAdapterToEdgeOperationalStatus,
  computeAdapterToDeviceOperationalStatus,
  computeAdapterNodeOperationalStatus,
} from './status-adapter-edge-operational.utils.ts'
import { OperationalStatus } from '../types/status.types'

describe('adapter-edge-operational-status.utils', () => {
  describe('adapterHasNorthboundMappings', () => {
    it.each([
      {
        description: 'should return true if adapter has northbound mappings',
        config: { northboundMappings: [{ id: 'mapping-1' }] as any },
        expected: true,
      },
      {
        description: 'should return false if adapter has no northbound mappings',
        config: { northboundMappings: [] },
        expected: false,
      },
      {
        description: 'should return false if northbound mappings is undefined',
        config: {},
        expected: false,
      },
      {
        description: 'should return false if config is undefined',
        config: undefined,
        expected: false,
      },
    ])('$description', ({ config, expected }) => {
      expect(adapterHasNorthboundMappings(config)).toBe(expected)
    })
  })

  describe('adapterHasSouthboundMappings', () => {
    it('should return true if adapter has southbound mappings', () => {
      const config: AdapterConfig = {
        southboundMappings: [{ id: 'mapping-1' }] as any,
      }
      expect(adapterHasSouthboundMappings(config)).toBe(true)
    })

    it('should return false if adapter has no southbound mappings', () => {
      const config: AdapterConfig = {
        southboundMappings: [],
      }
      expect(adapterHasSouthboundMappings(config)).toBe(false)
    })

    it('should return false if config is undefined', () => {
      expect(adapterHasSouthboundMappings(undefined)).toBe(false)
    })
  })

  describe('getDeviceTagNames', () => {
    it('should return set of tag names', () => {
      const config: AdapterConfig = {
        tags: [
          { name: 'temperature', description: 'Temp sensor' } as any,
          { name: 'pressure', description: 'Pressure sensor' } as any,
        ],
      }
      const result = getDeviceTagNames(config)
      expect(result.size).toBe(2)
      expect(result.has('temperature')).toBe(true)
      expect(result.has('pressure')).toBe(true)
    })

    it('should return empty set if no tags', () => {
      const config: AdapterConfig = {}
      expect(getDeviceTagNames(config).size).toBe(0)
    })

    it('should return empty set if config is undefined', () => {
      expect(getDeviceTagNames(undefined).size).toBe(0)
    })
  })

  describe('combinerHasValidAdapterTagMappings', () => {
    const deviceTags = new Set(['temperature', 'pressure'])

    it('should return false if combiner has no mappings', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Test Combiner',
        sources: {
          items: [{ id: 'adapter-1', type: EntityType.ADAPTER }],
        },
        mappings: { items: [] },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(false)
    })

    it('should return false if combiner has no adapter source', () => {
      const combiner: Combiner = {
        id: 'combiner-1',
        name: 'Test Combiner',
        sources: {
          items: [{ id: 'bridge-1', type: EntityType.BRIDGE }],
        },
        mappings: {
          items: [
            {
              id: 'mapping-1',
              sources: {
                primary: { id: 'temperature', type: DataIdentifierReference.type.TAG },
              },
              destination: { topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(false)
    })

    it('should return true if mapping uses device tag in primary source', () => {
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
              sources: {
                primary: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
              },
              destination: { topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(true)
    })

    it('should return false if mapping uses non-existent tag', () => {
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
              sources: {
                primary: { id: 'non-existent-tag', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
              },
              destination: { topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(false)
    })

    it('should return true if mapping uses device tag in tags array', () => {
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
              sources: {
                primary: { id: 'other-source', type: DataIdentifierReference.type.TOPIC_FILTER },
                tags: ['temperature', 'humidity'],
              },
              destination: { topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(true)
    })

    it('should return true if at least one mapping is valid (mixed scenario)', () => {
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
              sources: {
                primary: { id: 'non-existent', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
              },
              destination: { topic: 'test/topic1' },
              instructions: [],
            },
            {
              id: 'mapping-2',
              sources: {
                primary: { id: 'pressure', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
              },
              destination: { topic: 'test/topic2' },
              instructions: [],
            },
          ],
        },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(true)
    })

    it('should return false if primary is not a TAG type', () => {
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
              sources: {
                primary: { id: 'some-topic', type: DataIdentifierReference.type.TOPIC_FILTER },
              },
              destination: { topic: 'test/topic' },
              instructions: [],
            },
          ],
        },
      }
      expect(combinerHasValidAdapterTagMappings(combiner, deviceTags, 'adapter-1')).toBe(false)
    })
  })

  describe('computeAdapterToCombinerOperationalStatus', () => {
    const adapterConfig: AdapterConfig = {
      tags: [
        { name: 'temperature', description: 'Temp', definition: {} },
        { name: 'pressure', description: 'Pressure', definition: {} },
      ],
    }

    it.each([
      {
        description: 'should return ERROR if source is not an adapter node',
        source: {
          id: 'bridge-1',
          type: NodeTypes.BRIDGE_NODE,
          data: {},
          position: { x: 0, y: 0 },
        },
        target: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Combiner',
            sources: { items: [] },
            mappings: { items: [] },
          },
          position: { x: 0, y: 0 },
        },
        config: adapterConfig,
        expected: OperationalStatus.ERROR,
      },
      {
        description: 'should return ERROR if target is not a combiner node',
        source: {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          data: { id: 'adapter-1' },
          position: { x: 0, y: 0 },
        },
        target: {
          id: 'edge-1',
          type: NodeTypes.EDGE_NODE,
          data: { label: 'Edge' },
          position: { x: 0, y: 0 },
        },
        config: adapterConfig,
        expected: OperationalStatus.ERROR,
      },
      {
        description: 'should return ERROR if combiner has no adapter source',
        source: {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          data: { id: 'adapter-1' },
          position: { x: 0, y: 0 },
        },
        target: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Combiner',
            sources: {
              items: [{ id: 'bridge-1', type: EntityType.BRIDGE, name: 'Bridge' }],
            },
            mappings: { items: [] },
          },
          position: { x: 0, y: 0 },
        },
        config: adapterConfig,
        expected: OperationalStatus.ERROR,
      },
      {
        description: 'should return INACTIVE if device has no tags',
        source: {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          data: { id: 'adapter-1' },
          position: { x: 0, y: 0 },
        },
        target: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Combiner',
            sources: {
              items: [{ id: 'adapter-1', type: EntityType.ADAPTER }],
            },
            mappings: {
              items: [
                {
                  id: 'mapping-1',
                  sources: {
                    primary: { id: 'temperature', type: DataIdentifierReference.type.TAG },
                  },
                  destination: { topic: 'test/topic' },
                  instructions: [],
                },
              ],
            },
          },
          position: { x: 0, y: 0 },
        },
        config: {},
        expected: OperationalStatus.INACTIVE,
      },
      {
        description: 'should return INACTIVE if combiner has no valid tag mappings',
        source: {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          data: { id: 'adapter-1' },
          position: { x: 0, y: 0 },
        },
        target: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Combiner',
            sources: {
              items: [{ id: 'adapter-1', type: EntityType.ADAPTER }],
            },
            mappings: { items: [] },
          },
          position: { x: 0, y: 0 },
        },
        config: adapterConfig,
        expected: OperationalStatus.INACTIVE,
      },
      {
        description: 'should return ACTIVE if combiner has valid tag mappings',
        source: {
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          data: { id: 'adapter-1' },
          position: { x: 0, y: 0 },
        },
        target: {
          id: 'combiner-1',
          type: NodeTypes.COMBINER_NODE,
          data: {
            id: 'combiner-1',
            name: 'Combiner',
            sources: {
              items: [{ id: 'adapter-1', type: EntityType.ADAPTER }],
            },
            mappings: {
              items: [
                {
                  id: 'mapping-1',
                  sources: {
                    primary: { id: 'temperature', type: DataIdentifierReference.type.TAG, scope: 'adapter-1' },
                  },
                  destination: { topic: 'test/topic' },
                  instructions: [],
                },
              ],
            },
          },
          position: { x: 0, y: 0 },
        },
        config: adapterConfig,
        expected: OperationalStatus.ACTIVE,
      },
    ])('$description', ({ source, target, config, expected }) => {
      expect(computeAdapterToCombinerOperationalStatus(source as Node, target as Node, config)).toBe(expected)
    })
  })

  describe('computeAdapterToEdgeOperationalStatus', () => {
    it('should return ACTIVE if adapter has northbound mappings', () => {
      const config: AdapterConfig = {
        northboundMappings: [{ id: 'mapping-1' }] as any,
      }
      expect(computeAdapterToEdgeOperationalStatus(config)).toBe(OperationalStatus.ACTIVE)
    })

    it('should return INACTIVE if adapter has no northbound mappings', () => {
      const config: AdapterConfig = {
        northboundMappings: [],
      }
      expect(computeAdapterToEdgeOperationalStatus(config)).toBe(OperationalStatus.INACTIVE)
    })
  })

  describe('computeAdapterToDeviceOperationalStatus', () => {
    it('should return ACTIVE if adapter has southbound mappings', () => {
      const config: AdapterConfig = {
        southboundMappings: [{ id: 'mapping-1' }] as any,
      }
      expect(computeAdapterToDeviceOperationalStatus(config)).toBe(OperationalStatus.ACTIVE)
    })

    it('should return INACTIVE if adapter has no southbound mappings', () => {
      const config: AdapterConfig = {
        southboundMappings: [],
      }
      expect(computeAdapterToDeviceOperationalStatus(config)).toBe(OperationalStatus.INACTIVE)
    })
  })

  describe('computeAdapterNodeOperationalStatus', () => {
    it.each([
      {
        description: 'should return ACTIVE for unidirectional adapter with northbound mappings',
        config: {
          northboundMappings: [{ id: 'mapping-1' }] as any,
        },
        isBidirectional: false,
        expected: OperationalStatus.ACTIVE,
      },
      {
        description: 'should return INACTIVE for unidirectional adapter without northbound mappings',
        config: {
          northboundMappings: [],
        },
        isBidirectional: false,
        expected: OperationalStatus.INACTIVE,
      },
      {
        description: 'should return INACTIVE for bidirectional adapter with only northbound mappings',
        config: {
          northboundMappings: [{ id: 'mapping-1' }] as any,
          southboundMappings: [],
        },
        isBidirectional: true,
        expected: OperationalStatus.INACTIVE,
      },
      {
        description: 'should return INACTIVE for bidirectional adapter with only southbound mappings',
        config: {
          northboundMappings: [],
          southboundMappings: [{ id: 'mapping-1' }] as any,
        },
        isBidirectional: true,
        expected: OperationalStatus.INACTIVE,
      },
      {
        description: 'should return ACTIVE for bidirectional adapter with both north and south mappings',
        config: {
          northboundMappings: [{ id: 'mapping-1' }] as any,
          southboundMappings: [{ id: 'mapping-2' }] as any,
        },
        isBidirectional: true,
        expected: OperationalStatus.ACTIVE,
      },
    ])('$description', ({ config, isBidirectional, expected }) => {
      expect(computeAdapterNodeOperationalStatus(config, isBidirectional)).toBe(expected)
    })
  })
})
