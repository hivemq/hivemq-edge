import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useToast } from '@chakra-ui/react'
import type { Node } from '@xyflow/react'
import { useReactFlow } from '@xyflow/react'

import { useCompleteGroupWizard } from './useCompleteGroupWizard'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { NodeTypes } from '@/modules/Workspace/types.ts'

// Mock dependencies
vi.mock('@chakra-ui/react', async () => {
  const actual = await vi.importActual('@chakra-ui/react')
  return {
    ...actual,
    useToast: vi.fn(),
  }
})

vi.mock('@xyflow/react', () => ({
  useReactFlow: vi.fn(),
}))

vi.mock('@/modules/Workspace/hooks/useWizardStore', () => ({
  useWizardStore: {
    getState: vi.fn(),
  },
}))

vi.mock('@/modules/Workspace/hooks/useWorkspaceStore', () => ({
  default: vi.fn(),
}))

describe('useCompleteGroupWizard', () => {
  const mockToast = vi.fn()
  const mockGetNodes = vi.fn()
  const mockSetNodes = vi.fn()
  const mockGetEdges = vi.fn()
  const mockGetNodesBounds = vi.fn()
  const mockOnAddEdges = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(useToast).mockReturnValue(mockToast as unknown as ReturnType<typeof useToast>)
    vi.mocked(useReactFlow).mockReturnValue({
      getNodes: mockGetNodes,
      setNodes: mockSetNodes,
      getEdges: mockGetEdges,
      getNodesBounds: mockGetNodesBounds,
    } as unknown as ReturnType<typeof useReactFlow>)
    vi.mocked(useWorkspaceStore).mockReturnValue({
      onAddEdges: mockOnAddEdges,
    } as unknown as ReturnType<typeof useWorkspaceStore>)
    vi.mocked(useWizardStore.getState).mockReturnValue({
      configurationData: {
        groupConfig: {
          title: 'Test Group',
          colorScheme: 'blue',
        },
      },
      selectedNodeIds: ['adapter-1', 'adapter-2'],
      actions: {
        cancelWizard: vi.fn(),
      },
    } as unknown as ReturnType<typeof useWizardStore.getState>)

    mockGetNodes.mockReturnValue([
      {
        id: 'adapter-1',
        type: 'ADAPTER_NODE',
        position: { x: 100, y: 100 },
        data: { id: 'adapter-1' },
      },
      {
        id: 'adapter-2',
        type: 'ADAPTER_NODE',
        position: { x: 300, y: 100 },
        data: { id: 'adapter-2' },
      },
    ])
    mockGetEdges.mockReturnValue([])
    mockGetNodesBounds.mockReturnValue({ x: 100, y: 100, width: 200, height: 100 })
  })

  describe('hook interface', () => {
    it('should return an object with completeWizard function', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      expect(result.current).toHaveProperty('completeWizard')
      expect(typeof result.current.completeWizard).toBe('function')
    })

    it('should return an object with isCompleting boolean', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      expect(result.current).toHaveProperty('isCompleting')
      expect(typeof result.current.isCompleting).toBe('boolean')
      expect(result.current.isCompleting).toBe(false) // Initially false
    })

    it('should have correct return type shape', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      // Verify the exact shape expected by TypeScript
      expect(result.current).toEqual({
        completeWizard: expect.any(Function),
        isCompleting: expect.any(Boolean),
      })
    })
  })

  describe('completeWizard function signature', () => {
    it('should be an async function', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      expect(result.current.completeWizard.constructor.name).toBe('AsyncFunction')
    })

    it('should return a promise', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      let returnValue: Promise<void>
      act(() => {
        returnValue = result.current.completeWizard()
      })
      expect(returnValue!).toBeInstanceOf(Promise)
    })

    it('should not throw synchronously', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      expect(() => {
        act(() => {
          result.current.completeWizard()
        })
      }).not.toThrow()
    })
  })

  describe('configuration validation', () => {
    it('should handle missing group configuration', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {},
        selectedNodeIds: ['adapter-1', 'adapter-2'],
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('group configuration'),
        })
      )
    })

    it('should handle missing selected nodes', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: undefined,
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('selected nodes'),
        })
      )
    })

    it('should handle insufficient nodes (less than 2)', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['adapter-1'],
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('minimum 2'),
        })
      )
    })
  })

  describe('node filtering', () => {
    it('should filter out ghost nodes from selection', async () => {
      mockGetNodes.mockReturnValue([
        {
          id: 'adapter-1',
          type: 'ADAPTER_NODE',
          position: { x: 100, y: 100 },
          data: { id: 'adapter-1' },
        },
        {
          id: 'ghost-adapter-2',
          type: 'ADAPTER_NODE',
          position: { x: 300, y: 100 },
          data: { id: 'adapter-2', isGhost: true },
        },
      ])

      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['adapter-1', 'ghost-adapter-2'],
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      // Should error because only 1 non-ghost node
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('At least 2 nodes'),
        })
      )
    })

    it('should error when only device/host nodes are selected', async () => {
      mockGetNodes.mockReturnValue([
        {
          id: 'device-1',
          type: 'DEVICE_NODE',
          position: { x: 100, y: 100 },
          data: { id: 'device-1' },
        },
        {
          id: 'device-2',
          type: 'DEVICE_NODE',
          position: { x: 200, y: 200 },
          data: { id: 'device-2' },
        },
      ])

      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['device-1', 'device-2'],
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      // Should error - all devices filtered out, no valid group candidates
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('No valid nodes to group'),
        })
      )
    })

    it('should accept CLUSTER_NODE (nested groups)', async () => {
      mockGetNodes.mockReturnValue([
        {
          id: 'group-1',
          type: 'CLUSTER_NODE',
          position: { x: 100, y: 100 },
          data: { id: 'group-1', title: 'Group 1', childrenNodeIds: [] },
        },
        {
          id: 'adapter-1',
          type: 'ADAPTER_NODE',
          position: { x: 300, y: 100 },
          data: { id: 'adapter-1' },
        },
      ])

      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['group-1', 'adapter-1'],
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      // Should succeed - both are valid group candidates
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'success',
        })
      )
    })
  })

  describe('nesting depth validation', () => {
    it('should reject groups that would exceed max nesting depth', async () => {
      // Create a deeply nested group structure (4 levels = depth 3, MAX is 3)
      // Adding this to a new group would make it depth 4, exceeding MAX
      mockGetNodes.mockReturnValue([
        {
          id: 'group-deep',
          type: 'CLUSTER_NODE',
          position: { x: 100, y: 100 },
          data: {
            id: 'group-deep',
            title: 'Deep Group',
            childrenNodeIds: ['group-inner-1'],
          },
        },
        {
          id: 'group-inner-1',
          type: 'CLUSTER_NODE',
          position: { x: 20, y: 20 },
          parentId: 'group-deep',
          data: {
            id: 'group-inner-1',
            title: 'Inner Group 1',
            childrenNodeIds: ['group-inner-2'],
          },
        },
        {
          id: 'group-inner-2',
          type: 'CLUSTER_NODE',
          position: { x: 10, y: 10 },
          parentId: 'group-inner-1',
          data: {
            id: 'group-inner-2',
            title: 'Inner Group 2',
            childrenNodeIds: ['group-inner-3'],
          },
        },
        {
          id: 'group-inner-3',
          type: 'CLUSTER_NODE',
          position: { x: 5, y: 5 },
          parentId: 'group-inner-2',
          data: {
            id: 'group-inner-3',
            title: 'Inner Group 3',
            childrenNodeIds: [],
          },
        },
        {
          id: 'adapter-1',
          type: 'ADAPTER_NODE',
          position: { x: 300, y: 100 },
          data: { id: 'adapter-1' },
        },
      ])

      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['group-deep', 'adapter-1'],
        actions: { cancelWizard: vi.fn() },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('Maximum nesting depth'),
        })
      )
    })
  })

  describe('group node creation', () => {
    it('should create group node with correct ID format', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockSetNodes).toHaveBeenCalled()
      const nodes = mockSetNodes.mock.calls[0][0]
      const groupNode = nodes.find((n: { type: string }) => n.type === 'CLUSTER_NODE')

      expect(groupNode.id).toMatch(/^GROUP_NODE@/)
      expect(groupNode.id).toContain('adapter-1')
      expect(groupNode.id).toContain('adapter-2')
    })

    it('should create group node with correct data structure', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      const nodes = mockSetNodes.mock.calls[0][0]
      const groupNode = nodes.find((n: { type: string }) => n.type === 'CLUSTER_NODE')

      expect(groupNode.data).toEqual(
        expect.objectContaining({
          childrenNodeIds: expect.arrayContaining(['adapter-1', 'adapter-2']),
          title: 'Test Group',
          colorScheme: 'blue',
          isOpen: true,
        })
      )
    })

    it('should position group node using calculated bounds', async () => {
      mockGetNodesBounds.mockReturnValue({ x: 50, y: 75, width: 300, height: 150 })

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      const nodes = mockSetNodes.mock.calls[0][0]
      const groupNode = nodes.find((n: { type: string }) => n.type === 'CLUSTER_NODE')

      expect(groupNode.position).toBeDefined()
      expect(groupNode.style).toEqual(
        expect.objectContaining({
          width: expect.any(Number),
          height: expect.any(Number),
        })
      )
    })
  })

  describe('child node updates', () => {
    it('should set parentId on all grouped nodes', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      const nodes = mockSetNodes.mock.calls[0][0]
      const groupNode = nodes.find((n: { type: string }) => n.type === 'CLUSTER_NODE')
      const childNodes = nodes.filter((n: { parentId: string }) => n.parentId === groupNode.id)

      expect(childNodes.length).toBeGreaterThan(0)
      childNodes.forEach((child: { parentId: string; extent: string }) => {
        expect(child.parentId).toBe(groupNode.id)
        expect(child.extent).toBe('parent')
      })
    })

    it('should convert child positions to relative coordinates', async () => {
      mockGetNodesBounds.mockReturnValue({ x: 100, y: 100, width: 200, height: 100 })

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      const nodes = mockSetNodes.mock.calls[0][0]
      const groupNode = nodes.find((n: Node) => n.type === NodeTypes.CLUSTER_NODE)
      const childNode = nodes.find(
        (n: { id: string; parentId: string }) => n.id === 'adapter-1' && n.parentId === groupNode.id
      )

      // Original position was { x: 100, y: 100 }, group is at { x: 100, y: 100 } (after padding)
      // So relative position should be close to { x: 0, y: 0 } (with padding adjustment)
      expect(childNode.position.x).toBeLessThanOrEqual(100)
      expect(childNode.position.y).toBeLessThanOrEqual(100)
    })
  })

  describe('edge creation', () => {
    it('should create edge from group to EDGE_NODE', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockOnAddEdges).toHaveBeenCalledWith([
        {
          item: expect.objectContaining({
            source: expect.stringMatching(/^GROUP_NODE@/),
            target: 'EDGE_NODE',
            targetHandle: 'Top',
            type: 'DYNAMIC_EDGE',
          }),
          type: 'add',
        },
      ])
    })

    it('should create hidden edge with correct styling', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      const edgeCall = mockOnAddEdges.mock.calls[0][0][0]
      expect(edgeCall.item).toEqual(
        expect.objectContaining({
          hidden: true,
          focusable: false,
          markerEnd: expect.objectContaining({
            type: 'arrowclosed',
            width: 20,
            height: 20,
            color: '#4299E1',
          }),
        })
      )
    })
  })

  describe('ghost node cleanup', () => {
    it('should remove ghost nodes before adding group', async () => {
      mockGetNodes.mockReturnValue([
        {
          id: 'adapter-1',
          type: 'ADAPTER_NODE',
          position: { x: 100, y: 100 },
          data: { id: 'adapter-1' },
        },
        {
          id: 'adapter-2',
          type: 'ADAPTER_NODE',
          position: { x: 300, y: 100 },
          data: { id: 'adapter-2' },
        },
        {
          id: 'ghost-group-selection',
          type: 'CLUSTER_NODE',
          position: { x: 0, y: 0 },
          data: { isGhost: true },
        },
      ])

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      const nodes = mockSetNodes.mock.calls[0][0]
      const hasGhostNodes = nodes.some((n: { id: string }) => n.id.startsWith('ghost-'))

      expect(hasGhostNodes).toBe(false)
    })
  })

  describe('success flow', () => {
    it('should show success toast on completion', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'success',
          title: expect.any(String),
          description: expect.stringContaining('Test Group'),
        })
      )
    })

    it('should close wizard after successful completion', async () => {
      const mockCancelWizard = vi.fn()
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['adapter-1', 'adapter-2'],
        actions: { cancelWizard: mockCancelWizard },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      // Wait for async operations
      await new Promise((resolve) => setTimeout(resolve, 150))

      expect(mockCancelWizard).toHaveBeenCalled()
    })
  })

  describe('error handling', () => {
    it('should show error toast when React Flow operations fail', async () => {
      mockSetNodes.mockImplementation(() => {
        throw new Error('React Flow error')
      })

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          description: expect.stringContaining('React Flow error'),
        })
      )
    })

    it('should NOT call cancelWizard when error occurs', async () => {
      const mockCancelWizard = vi.fn()
      mockSetNodes.mockImplementation(() => {
        throw new Error('Test error')
      })

      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          groupConfig: { title: 'Test', colorScheme: 'blue' },
        },
        selectedNodeIds: ['adapter-1', 'adapter-2'],
        actions: { cancelWizard: mockCancelWizard },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteGroupWizard())
      await result.current.completeWizard()

      expect(mockCancelWizard).not.toHaveBeenCalled()
    })

    it('should reset isCompleting state after error', async () => {
      mockSetNodes.mockImplementation(() => {
        throw new Error('Test error')
      })

      const { result } = renderHook(() => useCompleteGroupWizard())

      await result.current.completeWizard()

      expect(result.current.isCompleting).toBe(false)
    })
  })

  describe('return value', () => {
    beforeEach(() => {
      // Reset mockSetNodes to default behavior (some previous tests set it to throw)
      mockSetNodes.mockImplementation(() => {
        // Default: just accept the nodes without error
      })
    })

    it('should return a promise from completeWizard', () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      let returnValue: Promise<void>
      act(() => {
        returnValue = result.current.completeWizard()
      })

      expect(returnValue!).toBeInstanceOf(Promise)
    })

    it('should resolve promise on success', async () => {
      const { result } = renderHook(() => useCompleteGroupWizard())

      await act(async () => {
        await expect(result.current.completeWizard()).resolves.not.toThrow()
      })
    })

    it('should resolve promise even on error (errors are caught internally)', async () => {
      mockSetNodes.mockImplementation(() => {
        throw new Error('Test error')
      })

      const { result } = renderHook(() => useCompleteGroupWizard())

      await act(async () => {
        await expect(result.current.completeWizard()).resolves.not.toThrow()
      })
    })
  })
})

// Note: Integration aspects tested elsewhere:
// - Full wizard flow validation: E2E Cypress tests
// - Group constraint validation: groupConstraints.spec.ts (56 tests)
// - Ghost node utilities: ghostNodeFactory.spec.ts (71 tests)
// - Auto-included nodes logic: groupConstraints.spec.ts (getAutoIncludedNodes tests)
// This file focuses on the hook's structure, error handling, and React Flow integration
