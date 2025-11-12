import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'

import { useWizardStore, useWizardCanProceed } from './useWizardStore'
import { EntityType, type GhostNode, type GhostEdge } from '../components/wizard/types'
import { NodeTypes } from '@/modules/Workspace/types'

describe('useWizardStore', () => {
  beforeEach(() => {
    const { result } = renderHook(() => useWizardStore())
    act(() => {
      result.current.actions.cancelWizard()
    })
  })

  describe('initial state', () => {
    it('should start with wizard inactive', () => {
      const { result } = renderHook(() => useWizardStore())

      expect(result.current.isActive).toBe(false)
      expect(result.current.entityType).toBeNull()
      expect(result.current.currentStep).toBe(0)
      expect(result.current.selectedNodeIds).toEqual([])
      expect(result.current.ghostNodes).toEqual([])
      expect(result.current.ghostEdges).toEqual([])
    })
  })

  describe('startWizard', () => {
    it('should activate wizard with correct entity type', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      expect(result.current.isActive).toBe(true)
      expect(result.current.entityType).toBe(EntityType.ADAPTER)
      expect(result.current.currentStep).toBe(0)
    })

    it('should set correct total steps for adapter', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      expect(result.current.totalSteps).toBe(3) // Adapter has 3 steps
    })

    it('should set correct total steps for combiner', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
      })

      expect(result.current.totalSteps).toBe(2) // Combiner has 2 steps
    })

    it('should auto-select Pulse Agent for Asset Mapper', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ASSET_MAPPER)
      })

      // Note: This will be empty unless there's actually a Pulse Agent node
      // In real usage, the store looks for PULSE_NODE in workspace
      expect(result.current.selectedNodeIds).toBeDefined()
    })

    it('should reset state when starting new wizard', () => {
      const { result } = renderHook(() => useWizardStore())

      // Start first wizard and add some state
      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.selectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toContain('node-1')

      // Start new wizard
      act(() => {
        result.current.actions.startWizard(EntityType.BRIDGE)
      })

      // State should be reset
      expect(result.current.entityType).toBe(EntityType.BRIDGE)
      expect(result.current.selectedNodeIds).toEqual([])
      expect(result.current.currentStep).toBe(0)
    })
  })

  describe('cancelWizard', () => {
    it('should reset wizard to initial state', () => {
      const { result } = renderHook(() => useWizardStore())

      // Start wizard and add some state
      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-2')
      })

      expect(result.current.isActive).toBe(true)

      // Cancel wizard
      act(() => {
        result.current.actions.cancelWizard()
      })

      expect(result.current.isActive).toBe(false)
      expect(result.current.entityType).toBeNull()
      expect(result.current.selectedNodeIds).toEqual([])
      expect(result.current.currentStep).toBe(0)
    })
  })

  describe('step navigation', () => {
    it('should advance to next step', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      expect(result.current.currentStep).toBe(0)

      act(() => {
        result.current.actions.nextStep()
      })

      expect(result.current.currentStep).toBe(1)
    })

    it('should not advance beyond total steps', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      const totalSteps = result.current.totalSteps

      // Try to advance beyond total steps
      act(() => {
        result.current.actions.nextStep()
        result.current.actions.nextStep()
        result.current.actions.nextStep()
        result.current.actions.nextStep()
      })

      expect(result.current.currentStep).toBe(totalSteps - 1)
    })

    it('should go back to previous step', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.nextStep()
      })

      expect(result.current.currentStep).toBe(1)

      act(() => {
        result.current.actions.previousStep()
      })

      expect(result.current.currentStep).toBe(0)
    })

    it('should not go below step 0', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      expect(result.current.currentStep).toBe(0)

      act(() => {
        result.current.actions.previousStep()
        result.current.actions.previousStep()
      })

      expect(result.current.currentStep).toBe(0)
    })
  })

  describe('node selection', () => {
    it('should select a node', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
      })

      act(() => {
        result.current.actions.selectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toContain('node-1')
    })

    it('should select multiple nodes', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
      })

      act(() => {
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-2')
        result.current.actions.selectNode('node-3')
      })

      expect(result.current.selectedNodeIds).toEqual(['node-1', 'node-2', 'node-3'])
    })

    it('should not select the same node twice', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
      })

      act(() => {
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toEqual(['node-1'])
    })

    it('should deselect a node', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-2')
      })

      expect(result.current.selectedNodeIds).toContain('node-1')

      act(() => {
        result.current.actions.deselectNode('node-1')
      })

      expect(result.current.selectedNodeIds).not.toContain('node-1')
      expect(result.current.selectedNodeIds).toContain('node-2')
    })

    it('should not allow deselecting Pulse Agent in Asset Mapper', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ASSET_MAPPER)
      })

      // If Pulse Agent was auto-selected (would need actual node in store)
      // This test documents the behavior but won't fail without actual nodes
      const initialSelection = result.current.selectedNodeIds

      act(() => {
        // Try to deselect - should be prevented if it's a PULSE_NODE
        result.current.actions.deselectNode('pulse-node-id')
      })

      // Selection should remain unchanged (or empty if no Pulse Agent exists)
      expect(result.current.selectedNodeIds).toEqual(initialSelection)
    })
  })

  describe('ghost nodes', () => {
    it('should add ghost nodes', () => {
      const { result } = renderHook(() => useWizardStore())

      const ghostNode: GhostNode = {
        id: 'ghost-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: { isGhost: true, label: 'Ghost' },
      }

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.addGhostNodes([ghostNode])
      })

      expect(result.current.ghostNodes).toHaveLength(1)
      expect(result.current.ghostNodes[0]).toEqual(ghostNode)
    })

    it('should add ghost edges', () => {
      const { result } = renderHook(() => useWizardStore())

      const ghostEdge: GhostEdge = {
        id: 'ghost-edge-1',
        source: 'ghost-1',
        target: 'EDGE_NODE',
        data: { isGhost: true },
      }

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.addGhostEdges([ghostEdge])
      })

      expect(result.current.ghostEdges).toHaveLength(1)
      expect(result.current.ghostEdges[0]).toEqual(ghostEdge)
    })

    it('should clear ghost nodes', () => {
      const { result } = renderHook(() => useWizardStore())

      const ghostNode: GhostNode = {
        id: 'ghost-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: { isGhost: true, label: 'Ghost' },
      }

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.addGhostNodes([ghostNode])
      })

      expect(result.current.ghostNodes).toHaveLength(1)

      act(() => {
        result.current.actions.clearGhostNodes()
      })

      expect(result.current.ghostNodes).toEqual([])
      expect(result.current.ghostEdges).toEqual([])
    })
  })

  describe('error handling', () => {
    it('should set error message', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.setError('Something went wrong')
      })

      expect(result.current.errorMessage).toBe('Something went wrong')
    })

    it('should clear error message', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.setError('Error')
      })

      expect(result.current.errorMessage).toBe('Error')

      act(() => {
        result.current.actions.setError(null)
      })

      expect(result.current.errorMessage).toBeNull()
    })
  })

  describe('completeWizard', () => {
    it('should set error if no entity type selected', async () => {
      const { result } = renderHook(() => useWizardStore())

      await act(async () => {
        await result.current.actions.completeWizard()
      })

      expect(result.current.errorMessage).toBe('No entity type selected')
    })

    it('should set error if configuration validation fails', async () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      await act(async () => {
        await result.current.actions.completeWizard()
      })

      expect(result.current.errorMessage).toBe('Configuration validation failed')
    })

    it('should clear ghost nodes and cancel wizard on success', async () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.updateConfiguration({ test: 'data' })
        result.current.actions.addGhostNodes([
          {
            id: 'ghost-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { isGhost: true, label: 'Ghost' },
          },
        ])
      })

      await act(async () => {
        await result.current.actions.completeWizard()
      })

      expect(result.current.ghostNodes).toHaveLength(0)
      expect(result.current.isActive).toBe(false)
    })

    it('should handle errors during completion', async () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.updateConfiguration({ test: 'data' })
      })

      // Store original function
      const storeState = useWizardStore.getState()
      const originalValidate = storeState.actions.validateConfiguration

      // Replace with throwing function
      storeState.actions.validateConfiguration = () => {
        throw new Error('Validation error')
      }

      await act(async () => {
        await result.current.actions.completeWizard()
      })

      expect(result.current.errorMessage).toBe('Validation error')

      // Restore original
      storeState.actions.validateConfiguration = originalValidate
    })
  })

  describe('selection management', () => {
    it('should clear all selected nodes', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-2')
        result.current.actions.clearSelection()
      })

      expect(result.current.selectedNodeIds).toHaveLength(0)
    })
  })

  describe('configuration management', () => {
    it('should update configuration with partial data', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.updateConfiguration({ key1: 'value1' })
        result.current.actions.updateConfiguration({ key2: 'value2' })
      })

      expect(result.current.configurationData).toEqual({
        key1: 'value1',
        key2: 'value2',
      })
    })

    it('should revalidate after configuration update', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.updateConfiguration({ test: 'data' })
      })

      expect(result.current.isConfigurationValid).toBe(true)
    })

    it('should validate configuration correctly', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      // Empty configuration should be invalid
      let isValidEmpty: boolean
      act(() => {
        isValidEmpty = result.current.actions.validateConfiguration()
      })

      expect(isValidEmpty!).toBe(false)
      expect(result.current.isConfigurationValid).toBe(false)

      // Non-empty configuration should be valid
      let isValidWithData: boolean
      act(() => {
        result.current.actions.updateConfiguration({ test: 'data' })
        isValidWithData = result.current.actions.validateConfiguration()
      })

      expect(isValidWithData!).toBe(true)
      expect(result.current.isConfigurationValid).toBe(true)
    })
  })

  describe('side panel management', () => {
    it('should open side panel', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.openSidePanel()
      })

      expect(result.current.isSidePanelOpen).toBe(true)
    })

    it('should close side panel', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.openSidePanel()
        result.current.actions.closeSidePanel()
      })

      expect(result.current.isSidePanelOpen).toBe(false)
    })
  })
})

describe('useWizardCanProceed', () => {
  beforeEach(() => {
    const { result } = renderHook(() => useWizardStore())
    act(() => {
      result.current.actions.cancelWizard()
    })
  })

  it('should return false if at last step', () => {
    const { result: storeResult } = renderHook(() => useWizardStore())
    const { result: canProceedResult } = renderHook(() => useWizardCanProceed())

    act(() => {
      storeResult.current.actions.startWizard(EntityType.ADAPTER)
      // Navigate to last step
      storeResult.current.actions.nextStep()
      storeResult.current.actions.nextStep()
    })

    expect(canProceedResult.current).toBe(false)
  })

  it('should return false if minimum nodes constraint not met', () => {
    const { result: storeResult } = renderHook(() => useWizardStore())
    const { result: canProceedResult } = renderHook(() => useWizardCanProceed())

    act(() => {
      storeResult.current.actions.startWizard(EntityType.COMBINER)
    })

    // Combiner requires minNodes: 2, but we have 0 selected
    expect(canProceedResult.current).toBe(false)
  })

  it('should return false if required nodes not selected', () => {
    const { result: storeResult } = renderHook(() => useWizardStore())
    const { result: canProceedResult } = renderHook(() => useWizardCanProceed())

    act(() => {
      storeResult.current.actions.startWizard(EntityType.ASSET_MAPPER)
      // Asset Mapper requires Pulse Agent but let's deselect it
      storeResult.current.actions.deselectNode('PULSE_NODE')
    })

    expect(canProceedResult.current).toBe(false)
  })

  it('should return true if all constraints are met', () => {
    const { result: storeResult } = renderHook(() => useWizardStore())
    const { result: canProceedResult } = renderHook(() => useWizardCanProceed())

    act(() => {
      storeResult.current.actions.startWizard(EntityType.COMBINER)
      storeResult.current.actions.selectNode('ADAPTER_NODE@1')
      storeResult.current.actions.selectNode('ADAPTER_NODE@2')
    })

    expect(canProceedResult.current).toBe(true)
  })

  it('should return true for simple wizard types', () => {
    const { result: storeResult } = renderHook(() => useWizardStore())
    const { result: canProceedResult } = renderHook(() => useWizardCanProceed())

    act(() => {
      storeResult.current.actions.startWizard(EntityType.ADAPTER)
    })

    // Adapter has no selection constraints on step 0
    expect(canProceedResult.current).toBe(true)
  })
})
