import { describe, expect } from 'vitest'
import type { Node } from '@xyflow/react'
import {
  filterContainerStyle,
  applySelectionFilter,
  applyEntityFilter,
  applyProtocolFilter,
  applyStatusFilter,
  applyQuickFilters,
  hideNodeWithFilters,
} from '@/modules/Workspace/components/filters/filters.utils.ts'
import type {
  ActiveFilter,
  Filter,
  FilterAdapterOption,
  FilterConfigurationOption,
  FilterEntitiesOption,
  FilterSelectionOption,
  FilterStatusOption,
} from '@/modules/Workspace/components/filters/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

describe('filterContainerStyle', () => {
  it('should update the style', async () => {
    expect(filterContainerStyle({ backgroundColor: 'red' })).toStrictEqual(
      expect.objectContaining({
        minWidth: 'var(--chakra-sizes-3xs)',
      })
    )
  })
})

describe('applySelectionFilter', () => {
  const mockNode: Node = { id: 'node-1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } }

  interface SelectionTestCase {
    description: string
    criteria: ActiveFilter<Array<FilterSelectionOption>> | undefined
    expected: boolean | undefined
  }

  const testCases: SelectionTestCase[] = [
    {
      description: 'undefined criteria',
      criteria: undefined,
      expected: undefined,
    },
    {
      description: 'inactive criteria',
      criteria: { isActive: false, filter: [{ id: 'node-1', type: NodeTypes.ADAPTER_NODE }] },
      expected: undefined,
    },
    {
      description: 'undefined filter',
      criteria: { isActive: true, filter: undefined },
      expected: undefined,
    },
    {
      description: 'empty filter array',
      criteria: { isActive: true, filter: [] },
      expected: undefined,
    },
    {
      description: 'matching id',
      criteria: { isActive: true, filter: [{ id: 'node-1', type: NodeTypes.ADAPTER_NODE }] },
      expected: true,
    },
    {
      description: 'non-matching id',
      criteria: { isActive: true, filter: [{ id: 'node-2', type: NodeTypes.ADAPTER_NODE }] },
      expected: false,
    },
    {
      description: 'multiple ids with match',
      criteria: {
        isActive: true,
        filter: [
          { id: 'node-2', type: NodeTypes.BRIDGE_NODE },
          { id: 'node-1', type: NodeTypes.ADAPTER_NODE },
        ],
      },
      expected: true,
    },
    {
      description: 'multiple ids without match',
      criteria: {
        isActive: true,
        filter: [
          { id: 'node-2', type: NodeTypes.BRIDGE_NODE },
          { id: 'node-3', type: NodeTypes.ADAPTER_NODE },
        ],
      },
      expected: false,
    },
  ]

  it.each(testCases)('should return $expected when $description', ({ criteria, expected }) => {
    expect(applySelectionFilter(mockNode, criteria)).toBe(expected)
  })
})

describe('applyEntityFilter', () => {
  const mockNode: Node = { id: 'node-1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } }

  interface EntityTestCase {
    description: string
    criteria: ActiveFilter<Array<FilterEntitiesOption>> | undefined
    expected: boolean | undefined
  }

  const testCases: EntityTestCase[] = [
    {
      description: 'undefined criteria',
      criteria: undefined,
      expected: undefined,
    },
    {
      description: 'inactive criteria',
      criteria: { isActive: false, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
      expected: undefined,
    },
    {
      description: 'undefined filter',
      criteria: { isActive: true, filter: undefined },
      expected: undefined,
    },
    {
      description: 'empty filter array',
      criteria: { isActive: true, filter: [] },
      expected: undefined,
    },
    {
      description: 'matching type',
      criteria: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
      expected: true,
    },
    {
      description: 'non-matching type',
      criteria: { isActive: true, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
      expected: false,
    },
    {
      description: 'multiple types with match',
      criteria: {
        isActive: true,
        filter: [
          { value: NodeTypes.BRIDGE_NODE, label: 'Bridge' },
          { value: NodeTypes.ADAPTER_NODE, label: 'Adapter' },
        ],
      },
      expected: true,
    },
  ]

  it.each(testCases)('should return $expected when $description', ({ criteria, expected }) => {
    expect(applyEntityFilter(mockNode, criteria)).toBe(expected)
  })
})

describe('applyProtocolFilter', () => {
  const adapterNode: Node = {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    data: { type: 'opcua', status: { connection: 'connected', runtime: 'running' } },
    position: { x: 0, y: 0 },
  }
  const bridgeNode: Node = {
    id: 'bridge-1',
    type: NodeTypes.BRIDGE_NODE,
    data: {},
    position: { x: 0, y: 0 },
  }

  interface ProtocolTestCase {
    description: string
    node: Node
    criteria: ActiveFilter<Array<FilterAdapterOption>> | undefined
    expected: boolean | undefined
  }

  const testCases: ProtocolTestCase[] = [
    {
      description: 'undefined criteria',
      node: adapterNode,
      criteria: undefined,
      expected: undefined,
    },
    {
      description: 'inactive criteria',
      node: adapterNode,
      criteria: { isActive: false, filter: [{ type: 'opcua', label: 'OPC UA' }] },
      expected: undefined,
    },
    {
      description: 'undefined filter',
      node: adapterNode,
      criteria: { isActive: true, filter: undefined },
      expected: undefined,
    },
    {
      description: 'empty filter array',
      node: adapterNode,
      criteria: { isActive: true, filter: [] },
      expected: undefined,
    },
    {
      description: 'matching adapter type',
      node: adapterNode,
      criteria: { isActive: true, filter: [{ type: 'opcua', label: 'OPC UA' }] },
      expected: true,
    },
    {
      description: 'non-matching adapter type',
      node: adapterNode,
      criteria: { isActive: true, filter: [{ type: 'mqtt', label: 'MQTT' }] },
      expected: false,
    },
    {
      description: 'multiple protocols with match',
      node: adapterNode,
      criteria: {
        isActive: true,
        filter: [
          { type: 'mqtt', label: 'MQTT' },
          { type: 'opcua', label: 'OPC UA' },
        ],
      },
      expected: true,
    },
    {
      description: 'non-adapter node',
      node: bridgeNode,
      criteria: { isActive: true, filter: [{ type: 'opcua', label: 'OPC UA' }] },
      expected: false,
    },
  ]

  it.each(testCases)('should return $expected when $description', ({ node, criteria, expected }) => {
    expect(applyProtocolFilter(node, criteria)).toBe(expected)
  })
})

describe('applyStatusFilter', () => {
  const adapterNode: Node = {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    data: { status: { connection: 'connected', runtime: 'running' } },
    position: { x: 0, y: 0 },
  }
  const bridgeNode: Node = {
    id: 'bridge-1',
    type: NodeTypes.BRIDGE_NODE,
    data: { status: { connection: 'disconnected', runtime: 'stopped' } },
    position: { x: 0, y: 0 },
  }

  interface StatusTestCase {
    description: string
    node: Node
    criteria: ActiveFilter<Array<FilterStatusOption>> | undefined
    expected: boolean | undefined
  }

  const testCases: StatusTestCase[] = [
    {
      description: 'undefined criteria',
      node: adapterNode,
      criteria: undefined,
      expected: undefined,
    },
    {
      description: 'inactive criteria',
      node: adapterNode,
      criteria: { isActive: false, filter: [{ status: 'connected', label: 'Connected' }] },
      expected: undefined,
    },
    {
      description: 'undefined filter',
      node: adapterNode,
      criteria: { isActive: true, filter: undefined },
      expected: undefined,
    },
    {
      description: 'empty filter array',
      node: adapterNode,
      criteria: { isActive: true, filter: [] },
      expected: undefined,
    },
    {
      description: 'matching adapter connection status',
      node: adapterNode,
      criteria: { isActive: true, filter: [{ status: 'connected', label: 'Connected' }] },
      expected: true,
    },
    {
      description: 'matching adapter runtime status',
      node: adapterNode,
      criteria: { isActive: true, filter: [{ status: 'running', label: 'Running' }] },
      expected: true,
    },
    {
      description: 'matching bridge connection status',
      node: bridgeNode,
      criteria: { isActive: true, filter: [{ status: 'disconnected', label: 'Disconnected' }] },
      expected: true,
    },
    {
      description: 'matching bridge runtime status',
      node: bridgeNode,
      criteria: { isActive: true, filter: [{ status: 'stopped', label: 'Stopped' }] },
      expected: true,
    },
    {
      description: 'non-matching status',
      node: adapterNode,
      criteria: { isActive: true, filter: [{ status: 'stopped', label: 'Stopped' }] },
      expected: false,
    },
    {
      description: 'multiple statuses with match',
      node: adapterNode,
      criteria: {
        isActive: true,
        filter: [
          { status: 'stopped', label: 'Stopped' },
          { status: 'running', label: 'Running' },
        ],
      },
      expected: true,
    },
  ]

  it.each(testCases)('should return $expected when $description', ({ node, criteria, expected }) => {
    expect(applyStatusFilter(node, criteria)).toBe(expected)
  })
})

describe('applyQuickFilters', () => {
  const mockNode: Node = {
    id: 'node-1',
    type: NodeTypes.ADAPTER_NODE,
    data: {},
    position: { x: 0, y: 0 },
  }

  it('should return undefined when no quick filters provided', () => {
    expect(applyQuickFilters(mockNode, [])).toBeUndefined()
  })

  it('should return undefined when all quick filters are inactive', () => {
    const quickFilters: FilterConfigurationOption[] = [
      { isActive: false, filter: {} as Filter, label: 'new quick filter' },
      { isActive: false, filter: {} as Filter, label: 'new quick filter 2' },
    ]
    expect(applyQuickFilters(mockNode, quickFilters)).toBeUndefined()
  })

  it('should return true when at least one quick filter matches', () => {
    const quickFilters: FilterConfigurationOption[] = [
      {
        isActive: true,
        filter: {
          entities: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
        },
        label: 'new quick filter',
      },
    ]
    expect(applyQuickFilters(mockNode, quickFilters)).toBe(true)
  })

  it('should return false when all quick filters hide the node', () => {
    const quickFilters: FilterConfigurationOption[] = [
      {
        isActive: true,
        filter: {
          entities: { isActive: true, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
        },
        label: 'new quick filter',
      },
    ]
    expect(applyQuickFilters(mockNode, quickFilters)).toBe(false)
  })

  it('should return true when multiple quick filters and at least one matches', () => {
    const quickFilters: FilterConfigurationOption[] = [
      {
        isActive: true,
        filter: {
          entities: { isActive: true, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
        },
        label: 'new quick filter',
      },
      {
        isActive: true,
        filter: {
          entities: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
        },
        label: 'new quick filter',
      },
    ]
    expect(applyQuickFilters(mockNode, quickFilters)).toBe(true)
  })
})

describe('hideNodeWithFilters', () => {
  const edgeNode: Node = {
    id: 'edge-1',
    type: NodeTypes.EDGE_NODE,
    data: {},
    position: { x: 0, y: 0 },
  }
  const adapterNode: Node = {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    data: { type: 'opcua' },
    position: { x: 0, y: 0 },
  }
  const emptyFilter: Filter = {}

  it('should always return false for EDGE_NODE', () => {
    const filter: Filter = {
      entities: { isActive: true, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
    }
    expect(hideNodeWithFilters(edgeNode, filter)).toBe(false)
  })

  it('should return false when no filters are defined', () => {
    expect(hideNodeWithFilters(adapterNode, emptyFilter)).toBe(false)
  })

  it('should return false when at least one filter matches', () => {
    const filter: Filter = {
      entities: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
    }
    expect(hideNodeWithFilters(adapterNode, filter)).toBe(false)
  })

  it('should return true when filters are active but none match', () => {
    const filter: Filter = {
      entities: { isActive: true, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
    }
    expect(hideNodeWithFilters(adapterNode, filter)).toBe(true)
  })

  it('should return false when all active filters match', () => {
    const filter: Filter = {
      entities: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
      protocols: { isActive: true, filter: [{ type: 'opcua', label: 'OPC UA' }] },
    }
    expect(hideNodeWithFilters(adapterNode, filter)).toBe(false)
  })

  it('should return true when multiple filters and not all match', () => {
    const filter: Filter = {
      entities: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
      protocols: { isActive: true, filter: [{ type: 'mqtt', label: 'MQTT' }] },
    }
    expect(hideNodeWithFilters(adapterNode, filter)).toBe(true)
  })

  it('should prioritize quick filters when provided', () => {
    const mainFilter: Filter = {
      entities: { isActive: true, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
    }
    const quickFilters: FilterConfigurationOption[] = [
      {
        isActive: true,
        filter: {
          entities: { isActive: true, filter: [{ value: NodeTypes.ADAPTER_NODE, label: 'Adapter' }] },
        },
        label: 'new quick filter',
      },
    ]
    expect(hideNodeWithFilters(adapterNode, mainFilter, quickFilters)).toBe(false)
  })

  it('should handle inactive filters correctly', () => {
    const filter: Filter = {
      entities: { isActive: false, filter: [{ value: NodeTypes.BRIDGE_NODE, label: 'Bridge' }] },
      protocols: { isActive: true, filter: [{ type: 'opcua', label: 'OPC UA' }] },
    }
    expect(hideNodeWithFilters(adapterNode, filter)).toBe(false)
  })

  it('should handle nodes without required data properties', () => {
    const nodeWithoutData: Node = {
      id: 'node-1',
      type: NodeTypes.ADAPTER_NODE,
      data: {},
      position: { x: 0, y: 0 },
    }
    const filter: Filter = {
      protocols: { isActive: true, filter: [{ type: 'opcua', label: 'OPC UA' }] },
    }
    expect(hideNodeWithFilters(nodeWithoutData, filter)).toBe(true)
  })
})
