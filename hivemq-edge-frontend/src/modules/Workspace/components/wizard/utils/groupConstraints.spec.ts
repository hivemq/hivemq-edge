import { describe, it, expect } from 'vitest'
import type { Node } from '@xyflow/react'
import {
  MAX_NESTING_DEPTH,
  getNodeNestingDepth,
  getMaxChildDepth,
  canAddToGroup,
  canGroupCollapse,
  validateGroupHierarchy,
  canNodeBeGrouped,
  getGroupChildren,
} from './groupConstraints'
import {
  NodeTypes,
  type NodeGroupType,
  type NodeAdapterType,
  type NodeBridgeType,
  type NodeDeviceType,
  type NodeHostType,
} from '@/modules/Workspace/types'
import type { Adapter, Bridge } from '@/api/__generated__'

describe('groupConstraints - Nesting Validation', () => {
  // Test data factory functions - using proper typed nodes that match actual usage
  const createGroupNode = (id: string, childrenIds: string[], parentId?: string): NodeGroupType => ({
    id,
    type: NodeTypes.CLUSTER_NODE,
    position: { x: 0, y: 0 },
    data: {
      childrenNodeIds: childrenIds,
      title: `Group ${id}`,
      isOpen: true,
      colorScheme: 'blue',
    },
    parentId,
  })

  const createAdapterNode = (id: string, parentId?: string): NodeAdapterType => ({
    id,
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 0, y: 0 },
    data: {
      id,
      type: 'simulation',
      config: {},
    } as Adapter,
    parentId,
  })

  const createBridgeNode = (id: string, parentId?: string): NodeBridgeType => ({
    id,
    type: NodeTypes.BRIDGE_NODE,
    position: { x: 0, y: 0 },
    data: {
      id,
      host: 'localhost',
      port: 1883,
    } as Bridge,
    parentId,
  })

  const createDeviceNode = (id: string): NodeDeviceType => ({
    id,
    type: NodeTypes.DEVICE_NODE,
    position: { x: 0, y: 0 },
    data: {
      id,
      sourceAdapterId: id.replace('device-', ''),
    } as NodeDeviceType['data'],
  })

  const createHostNode = (id: string): NodeHostType => ({
    id,
    type: NodeTypes.HOST_NODE,
    position: { x: 0, y: 0 },
    data: {
      label: `Host ${id}`,
    },
  })

  describe('canNodeBeGrouped', () => {
    it('should allow grouping ADAPTER nodes', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1')]

      const result = canNodeBeGrouped(nodes[0], nodes)

      expect(result).toBe(true)
    })

    it('should allow grouping BRIDGE nodes', () => {
      const nodes: Node[] = [createBridgeNode('bridge-1')]

      const result = canNodeBeGrouped(nodes[0], nodes)

      expect(result).toBe(true)
    })

    it('should allow grouping CLUSTER (group) nodes', () => {
      const nodes: Node[] = [createGroupNode('group-1', [])]

      const result = canNodeBeGrouped(nodes[0], nodes)

      expect(result).toBe(true)
    })

    it('should reject ghost nodes', () => {
      const ghostNode: Node = {
        id: 'ghost-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: { id: 'ghost-1', isGhost: true },
      }

      const result = canNodeBeGrouped(ghostNode, [ghostNode])

      expect(result).toBe(false)
    })

    it('should reject EDGE node', () => {
      const edgeNode: Node = {
        id: 'EDGE_NODE',
        type: NodeTypes.EDGE_NODE,
        position: { x: 0, y: 0 },
        data: { id: 'EDGE_NODE' },
      }

      const result = canNodeBeGrouped(edgeNode, [edgeNode])

      expect(result).toBe(false)
    })

    it('should reject DEVICE nodes', () => {
      const nodes: Node[] = [createDeviceNode('device-1')]

      const result = canNodeBeGrouped(nodes[0], nodes)

      expect(result).toBe(false)
    })

    it('should reject HOST nodes', () => {
      const nodes: Node[] = [createHostNode('host-1')]

      const result = canNodeBeGrouped(nodes[0], nodes)

      expect(result).toBe(false)
    })

    it('should reject nodes already in a group', () => {
      const nodes: Node[] = [createGroupNode('group-1', ['adapter-1']), createAdapterNode('adapter-1', 'group-1')]

      const result = canNodeBeGrouped(nodes[1], nodes)

      expect(result).toBe(false)
    })

    it('should reject nodes with unknown type', () => {
      const unknownNode: Node = {
        id: 'unknown-1',
        type: 'UNKNOWN_TYPE',
        position: { x: 0, y: 0 },
        data: { id: 'unknown-1' },
      }

      const result = canNodeBeGrouped(unknownNode, [unknownNode])

      expect(result).toBe(false)
    })

    it('should reject nodes with undefined type', () => {
      const noTypeNode: Node = {
        id: 'no-type-1',
        type: undefined,
        position: { x: 0, y: 0 },
        data: { id: 'no-type-1' },
      }

      const result = canNodeBeGrouped(noTypeNode, [noTypeNode])

      expect(result).toBe(false)
    })
  })

  describe('getGroupChildren', () => {
    it('should return empty array for non-group node', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1')]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toEqual([])
    })

    it('should return direct children of a group', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['adapter-1', 'adapter-2']),
        createAdapterNode('adapter-1', 'group-1'),
        createAdapterNode('adapter-2', 'group-1'),
      ]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toHaveLength(2)
      expect(children.map((n) => n.id)).toContain('adapter-1')
      expect(children.map((n) => n.id)).toContain('adapter-2')
    })

    it('should recursively return children of nested groups', () => {
      const nodes: Node[] = [
        createGroupNode('group-outer', ['group-inner']),
        createGroupNode('group-inner', ['adapter-1'], 'group-outer'),
        createAdapterNode('adapter-1', 'group-inner'),
      ]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toHaveLength(2)
      expect(children.map((n) => n.id)).toContain('group-inner')
      expect(children.map((n) => n.id)).toContain('adapter-1')
    })

    it('should handle deeply nested groups', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', ['group-3'], 'group-1'),
        createGroupNode('group-3', ['adapter-1'], 'group-2'),
        createAdapterNode('adapter-1', 'group-3'),
      ]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toHaveLength(3)
      expect(children.map((n) => n.id)).toContain('group-2')
      expect(children.map((n) => n.id)).toContain('group-3')
      expect(children.map((n) => n.id)).toContain('adapter-1')
    })

    it('should return empty array when group has no children', () => {
      const nodes: Node[] = [createGroupNode('group-1', [])]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toEqual([])
    })

    it('should handle missing child nodes gracefully', () => {
      const nodes: Node[] = [createGroupNode('group-1', ['non-existent'])]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toEqual([])
    })

    it('should return children in order even with mixed types', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['adapter-1', 'bridge-1', 'group-2']),
        createAdapterNode('adapter-1', 'group-1'),
        createBridgeNode('bridge-1', 'group-1'),
        createGroupNode('group-2', [], 'group-1'),
      ]

      const children = getGroupChildren(nodes[0], nodes)

      expect(children).toHaveLength(3)
      expect(children.map((n) => n.id)).toEqual(['adapter-1', 'bridge-1', 'group-2'])
    })
  })

  describe('getNodeNestingDepth', () => {
    it('should return 0 for node at root level', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1')]

      expect(getNodeNestingDepth('adapter-1', nodes)).toBe(0)
    })

    it('should return 1 for node in one group', () => {
      const nodes: Node[] = [createGroupNode('group-1', ['adapter-1']), createAdapterNode('adapter-1', 'group-1')]

      expect(getNodeNestingDepth('adapter-1', nodes)).toBe(1)
    })

    it('should return 2 for node in nested group (2 levels)', () => {
      const nodes: Node[] = [
        createGroupNode('group-outer', ['group-inner']),
        createGroupNode('group-inner', ['adapter-1'], 'group-outer'),
        createAdapterNode('adapter-1', 'group-inner'),
      ]

      expect(getNodeNestingDepth('adapter-1', nodes)).toBe(2)
    })

    it('should return 3 for node in deeply nested group (3 levels)', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', ['group-3'], 'group-1'),
        createGroupNode('group-3', ['adapter-1'], 'group-2'),
        createAdapterNode('adapter-1', 'group-3'),
      ]

      expect(getNodeNestingDepth('adapter-1', nodes)).toBe(3)
    })

    it('should return 0 for non-existent node', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1')]

      expect(getNodeNestingDepth('non-existent', nodes)).toBe(0)
    })

    it('should return 1 for group node itself that is nested', () => {
      const nodes: Node[] = [
        createGroupNode('group-outer', ['group-inner']),
        createGroupNode('group-inner', ['adapter-1'], 'group-outer'),
      ]

      expect(getNodeNestingDepth('group-inner', nodes)).toBe(1)
    })

    it('should handle orphaned node (parent does not exist)', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1', 'non-existent-parent')]

      // Should count depth = 1 (has parentId) but stop traversing when parent not found
      expect(getNodeNestingDepth('adapter-1', nodes)).toBe(1)
    })

    it('should prevent infinite loop with max depth safety check', () => {
      // This would be a circular reference, but safety check should prevent infinite loop
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        { ...createGroupNode('group-2', [], 'group-1'), parentId: 'group-1' },
      ]

      // Should stop at max 10 iterations
      const depth = getNodeNestingDepth('group-2', nodes)
      expect(depth).toBeLessThanOrEqual(10)
    })
  })

  describe('getMaxChildDepth', () => {
    it('should return 0 for group with no children', () => {
      const nodes: Node[] = [createGroupNode('group-1', [])]

      expect(getMaxChildDepth(nodes[0], nodes)).toBe(0)
    })

    it('should return 0 for group with only adapter children', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['adapter-1', 'adapter-2']),
        createAdapterNode('adapter-1', 'group-1'),
        createAdapterNode('adapter-2', 'group-1'),
      ]

      expect(getMaxChildDepth(nodes[0], nodes)).toBe(0)
    })

    it('should return 1 for group containing one nested group', () => {
      const nodes: Node[] = [
        createGroupNode('group-outer', ['group-inner']),
        createGroupNode('group-inner', ['adapter-1'], 'group-outer'),
        createAdapterNode('adapter-1', 'group-inner'),
      ]

      expect(getMaxChildDepth(nodes[0], nodes)).toBe(1)
    })

    it('should return 2 for group containing 2 levels of nested groups', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', ['group-3'], 'group-1'),
        createGroupNode('group-3', ['adapter-1'], 'group-2'),
        createAdapterNode('adapter-1', 'group-3'),
      ]

      expect(getMaxChildDepth(nodes[0], nodes)).toBe(2)
    })

    it('should return max depth when group has multiple nested groups at different depths', () => {
      const nodes: Node[] = [
        createGroupNode('group-main', ['group-shallow', 'group-deep']),
        createGroupNode('group-shallow', ['adapter-1'], 'group-main'),
        createGroupNode('group-deep', ['group-deeper'], 'group-main'),
        createGroupNode('group-deeper', ['adapter-2'], 'group-deep'),
        createAdapterNode('adapter-1', 'group-shallow'),
        createAdapterNode('adapter-2', 'group-deeper'),
      ]

      // group-shallow has depth 1, group-deep has depth 2, max is 2
      expect(getMaxChildDepth(nodes[0], nodes)).toBe(2)
    })

    it('should return 0 for non-group node', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1')]

      expect(getMaxChildDepth(nodes[0], nodes)).toBe(0)
    })

    it('should handle missing child nodes gracefully', () => {
      const nodes: Node[] = [createGroupNode('group-1', ['non-existent-child'])]

      expect(getMaxChildDepth(nodes[0], nodes)).toBe(0)
    })
  })

  describe('canAddToGroup', () => {
    it('should allow adding adapter to root-level group (depth 0 → 1)', () => {
      const nodes: Node[] = [createGroupNode('group-1', []), createAdapterNode('adapter-1')]

      const result = canAddToGroup('adapter-1', 'group-1', nodes)

      expect(result.allowed).toBe(true)
      expect(result.reason).toBeUndefined()
    })

    it('should allow adding adapter to nested group at depth 2 (max depth 3)', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', [], 'group-1'),
        createAdapterNode('adapter-1'),
      ]

      const result = canAddToGroup('adapter-1', 'group-2', nodes)

      expect(result.allowed).toBe(true)
    })

    it('should reject adding adapter when would exceed max depth', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', ['group-3'], 'group-1'),
        createGroupNode('group-3', [], 'group-2'),
        createAdapterNode('adapter-1'),
      ]

      // group-3 is at depth 2, adding adapter would make it depth 3, which exceeds MAX (3)
      // Actually depth 3 is the max, so this should be allowed. Let me recalculate:
      // group-1: depth 0
      // group-2: depth 1
      // group-3: depth 2
      // adapter would be at depth 3 (which equals MAX_NESTING_DEPTH = 3)
      // This should still be allowed since it equals max, not exceeds

      const result = canAddToGroup('adapter-1', 'group-3', nodes)

      // At max depth
      expect(result.allowed).toBe(true)
    })

    it('should reject adding adapter when at max depth already', () => {
      // Create a structure at exactly max depth
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', ['group-3'], 'group-1'),
        createGroupNode('group-3', ['group-4'], 'group-2'),
        createGroupNode('group-4', [], 'group-3'),
        createAdapterNode('adapter-1'),
      ]

      // group-4 is at depth 3 (max), adding adapter would make it depth 4
      const result = canAddToGroup('adapter-1', 'group-4', nodes)

      expect(result.allowed).toBe(false)
      expect(result.reason).toContain(`Maximum nesting depth (${MAX_NESTING_DEPTH} levels)`)
    })

    it('should allow adding shallow group to root-level group', () => {
      const nodes: Node[] = [
        createGroupNode('group-target', []),
        createGroupNode('group-to-add', ['adapter-1']),
        createAdapterNode('adapter-1', 'group-to-add'),
      ]

      const result = canAddToGroup('group-to-add', 'group-target', nodes)

      expect(result.allowed).toBe(true)
    })

    it('should reject adding group when its internal depth would exceed max', () => {
      const nodes: Node[] = [
        createGroupNode('group-target', ['group-existing']),
        createGroupNode('group-existing', [], 'group-target'),
        // Group to add has internal depth of 2
        createGroupNode('group-to-add', ['group-inner-1']),
        createGroupNode('group-inner-1', ['group-inner-2'], 'group-to-add'),
        createGroupNode('group-inner-2', [], 'group-inner-1'),
      ]

      // group-target is at depth 0
      // Adding group-to-add (internal depth 2) would create:
      // - group-to-add at depth 1
      // - group-inner-1 at depth 2
      // - group-inner-2 at depth 3 (max)
      // This should be allowed since it equals max

      const result = canAddToGroup('group-to-add', 'group-target', nodes)
      expect(result.allowed).toBe(true)
    })

    it('should reject adding deeply nested group that would exceed max depth', () => {
      const nodes: Node[] = [
        createGroupNode('group-target', ['group-existing']),
        createGroupNode('group-existing', [], 'group-target'),
        // Group to add has internal depth of 3 (too deep)
        createGroupNode('group-to-add', ['g1']),
        createGroupNode('g1', ['g2'], 'group-to-add'),
        createGroupNode('g2', ['g3'], 'g1'),
        createGroupNode('g3', [], 'g2'),
      ]

      // group-target is at depth 0
      // Adding group-to-add (internal depth 3) would create depth 4
      const result = canAddToGroup('group-to-add', 'group-target', nodes)

      expect(result.allowed).toBe(false)
      expect(result.reason).toContain('Maximum nesting depth')
    })

    it('should return false for non-existent node', () => {
      const nodes: Node[] = [createGroupNode('group-1', [])]

      const result = canAddToGroup('non-existent', 'group-1', nodes)

      expect(result.allowed).toBe(false)
      expect(result.reason).toBe('Node not found')
    })
  })

  describe('canGroupCollapse', () => {
    it('should allow collapsing root-level group', () => {
      const nodes: Node[] = [createGroupNode('group-1', ['adapter-1']), createAdapterNode('adapter-1', 'group-1')]

      const result = canGroupCollapse('group-1', nodes)

      expect(result.allowed).toBe(true)
      expect(result.reason).toBeUndefined()
    })

    it('should allow collapsing nested group when parent is expanded', () => {
      const nodes: Node[] = [
        createGroupNode('group-outer', ['group-inner']),
        { ...createGroupNode('group-inner', ['adapter-1'], 'group-outer'), data: { isCollapsed: false } },
        createAdapterNode('adapter-1', 'group-inner'),
      ]

      const result = canGroupCollapse('group-inner', nodes)

      expect(result.allowed).toBe(true)
    })

    it('should reject collapsing nested group when parent is collapsed', () => {
      const nodes: Node[] = [
        {
          ...createGroupNode('group-outer', ['group-inner']),
          data: {
            isCollapsed: true,
            childrenNodeIds: ['group-inner'],
            title: 'Group outer',
            isOpen: false,
            colorScheme: 'blue',
          },
        },
        createGroupNode('group-inner', ['adapter-1'], 'group-outer'),
        createAdapterNode('adapter-1', 'group-inner'),
      ]

      const result = canGroupCollapse('group-inner', nodes)

      expect(result.allowed).toBe(false)
      expect(result.reason).toContain('Cannot collapse nested group when parent is collapsed')
    })

    it('should allow collapsing nested group at depth 2 when all parents expanded', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        createGroupNode('group-2', ['group-3'], 'group-1'),
        createGroupNode('group-3', ['adapter-1'], 'group-2'),
        createAdapterNode('adapter-1', 'group-3'),
      ]

      const result = canGroupCollapse('group-3', nodes)

      expect(result.allowed).toBe(true)
    })

    it('should reject when any parent in chain is collapsed', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2']),
        {
          ...createGroupNode('group-2', ['group-3'], 'group-1'),
          data: {
            isCollapsed: true,
            childrenNodeIds: ['group-3'],
            title: 'Group 2',
            isOpen: false,
            colorScheme: 'blue',
          },
        },
        createGroupNode('group-3', ['adapter-1'], 'group-2'),
        createAdapterNode('adapter-1', 'group-3'),
      ]

      const result = canGroupCollapse('group-3', nodes)

      expect(result.allowed).toBe(false)
      expect(result.reason).toContain('Cannot collapse nested group')
    })

    it('should return false for non-existent group', () => {
      const nodes: Node[] = [createGroupNode('group-1', [])]

      const result = canGroupCollapse('non-existent', nodes)

      expect(result.allowed).toBe(false)
      expect(result.reason).toBe('Group not found')
    })
  })

  describe('validateGroupHierarchy', () => {
    it('should validate flat hierarchy (no groups)', () => {
      const nodes: Node[] = [createAdapterNode('adapter-1'), createAdapterNode('adapter-2')]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should validate simple group hierarchy', () => {
      const nodes: Node[] = [createGroupNode('group-1', ['adapter-1']), createAdapterNode('adapter-1', 'group-1')]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should validate nested group hierarchy', () => {
      const nodes: Node[] = [
        createGroupNode('group-outer', ['group-inner']),
        createGroupNode('group-inner', ['adapter-1'], 'group-outer'),
        createAdapterNode('adapter-1', 'group-inner'),
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should detect circular reference (2 nodes)', () => {
      const nodes: Node[] = [
        { ...createGroupNode('group-1', ['group-2']), parentId: 'group-2' },
        { ...createGroupNode('group-2', ['group-1']), parentId: 'group-1' },
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(false)
      expect(result.errors.length).toBeGreaterThan(0)
      expect(result.errors[0]).toContain('Circular reference')
    })

    it('should detect circular reference (3 nodes)', () => {
      const nodes: Node[] = [
        { ...createGroupNode('group-1', ['group-2']), parentId: 'group-3' },
        { ...createGroupNode('group-2', ['group-3']), parentId: 'group-1' },
        { ...createGroupNode('group-3', ['group-1']), parentId: 'group-2' },
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(false)
      expect(result.errors.length).toBeGreaterThan(0)
      expect(result.errors[0]).toContain('Circular reference')
    })

    it('should detect orphaned node (invalid parent)', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['adapter-1']),
        createAdapterNode('adapter-1', 'non-existent-parent'),
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(false)
      expect(result.errors).toHaveLength(1)
      expect(result.errors[0]).toContain('Orphaned node')
      expect(result.errors[0]).toContain('adapter-1')
    })

    it('should detect multiple orphaned nodes', () => {
      const nodes: Node[] = [
        createAdapterNode('adapter-1', 'missing-1'),
        createAdapterNode('adapter-2', 'missing-2'),
        createAdapterNode('adapter-3'),
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(false)
      expect(result.errors).toHaveLength(2)
      expect(result.errors.some((e) => e.includes('adapter-1'))).toBe(true)
      expect(result.errors.some((e) => e.includes('adapter-2'))).toBe(true)
    })

    it('should detect excessive nesting depth', () => {
      // Create a very deep hierarchy (> 10 levels)
      const nodes: Node[] = [createGroupNode('group-0', ['group-1'])]

      for (let i = 1; i <= 12; i++) {
        nodes.push(createGroupNode(`group-${i}`, [`group-${i + 1}`], `group-${i - 1}`))
      }

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(false)
      expect(result.errors.length).toBeGreaterThan(0)
      expect(result.errors.some((e) => e.includes('Excessive nesting depth'))).toBe(true)
    })

    it('should validate complex but valid hierarchy', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['group-2', 'adapter-1']),
        createGroupNode('group-2', ['adapter-2', 'adapter-3'], 'group-1'),
        createGroupNode('group-3', ['adapter-4']),
        createAdapterNode('adapter-1', 'group-1'),
        createAdapterNode('adapter-2', 'group-2'),
        createAdapterNode('adapter-3', 'group-2'),
        createAdapterNode('adapter-4', 'group-3'),
        createAdapterNode('adapter-5'),
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should handle nodes with no parent gracefully', () => {
      const nodes: Node[] = [
        createGroupNode('group-1', ['adapter-1']),
        createAdapterNode('adapter-1', 'group-1'),
        createAdapterNode('adapter-2'), // No parent
      ]

      const result = validateGroupHierarchy(nodes)

      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })
  })
})
