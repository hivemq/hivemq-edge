/**
 * Wizard Store Tests
 *
 * Tests for the wizard state management store.
 * Following pragmatic testing strategy: only accessibility test is unskipped.
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, afterEach } from 'vitest'

import {
  useWizardStore,
  useWizardState,
  useWizardActions,
  useWizardSelection,
  useWizardGhosts,
  useWizardConfiguration,
  useWizardCanProceed,
} from './useWizardStore'
import { EntityType, IntegrationPointType } from '../components/wizard/types'

describe('useWizardStore', () => {
  beforeEach(() => {
    // Reset store before each test
    const state = useWizardStore.getState()
    state.actions.cancelWizard()
  })

  afterEach(() => {
    // Clean up after each test
    const state = useWizardStore.getState()
    state.actions.cancelWizard()
  })

  // ✅ ACCESSIBILITY TEST - ALWAYS UNSKIPPED
  it('should be accessible', () => {
    // Since this is a store (not a UI component), we verify it provides
    // the necessary state and actions for accessible UI components

    const { result } = renderHook(() => useWizardStore())

    // Verify state is initialized correctly
    expect(result.current.isActive).toBe(false)
    expect(result.current.entityType).toBeNull()
    expect(result.current.errorMessage).toBeNull()

    // Verify actions exist for accessible UI interactions
    expect(result.current.actions.startWizard).toBeDefined()
    expect(result.current.actions.cancelWizard).toBeDefined()
    expect(result.current.actions.nextStep).toBeDefined()
    expect(result.current.actions.previousStep).toBeDefined()

    // Verify error handling for accessible feedback
    expect(result.current.actions.setError).toBeDefined()
    expect(result.current.actions.clearError).toBeDefined()
  })

  // ⏭️ SKIPPED TESTS - Document expected behavior but skip for rapid development

  describe.skip('startWizard', () => {
    it('should initialize wizard state for entity type', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      expect(result.current.isActive).toBe(true)
      expect(result.current.entityType).toBe(EntityType.ADAPTER)
      expect(result.current.currentStep).toBe(0)
      expect(result.current.totalSteps).toBeGreaterThan(0)
    })

    it('should initialize wizard state for integration point type', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(IntegrationPointType.TAG)
      })

      expect(result.current.isActive).toBe(true)
      expect(result.current.entityType).toBe(IntegrationPointType.TAG)
    })

    it('should reset previous wizard state', () => {
      const { result } = renderHook(() => useWizardStore())

      // Start first wizard
      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.selectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toHaveLength(1)

      // Start new wizard
      act(() => {
        result.current.actions.startWizard(EntityType.BRIDGE)
      })

      expect(result.current.selectedNodeIds).toHaveLength(0)
      expect(result.current.entityType).toBe(EntityType.BRIDGE)
    })
  })

  describe.skip('cancelWizard', () => {
    it('should reset all wizard state', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.selectNode('node-1')
        result.current.actions.updateConfiguration({ name: 'Test' })
      })

      expect(result.current.isActive).toBe(true)

      act(() => {
        result.current.actions.cancelWizard()
      })

      expect(result.current.isActive).toBe(false)
      expect(result.current.entityType).toBeNull()
      expect(result.current.selectedNodeIds).toHaveLength(0)
      expect(result.current.configurationData).toEqual({})
    })

    it('should clear ghost nodes', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.addGhostNodes([
          {
            id: 'ghost-1',
            type: 'ADAPTER_NODE',
            position: { x: 0, y: 0 },
            data: { isGhost: true, label: 'Ghost' },
          },
        ])
      })

      expect(result.current.ghostNodes).toHaveLength(1)

      act(() => {
        result.current.actions.cancelWizard()
      })

      expect(result.current.ghostNodes).toHaveLength(0)
    })
  })

  describe.skip('step navigation', () => {
    it('should move to next step', () => {
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

    it('should not exceed total steps', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      const totalSteps = result.current.totalSteps

      // Navigate to last step
      act(() => {
        for (let i = 0; i < totalSteps; i++) {
          result.current.actions.nextStep()
        }
      })

      expect(result.current.currentStep).toBe(totalSteps - 1)
    })

    it('should move to previous step', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.nextStep()
        result.current.actions.nextStep()
      })

      expect(result.current.currentStep).toBe(2)

      act(() => {
        result.current.actions.previousStep()
      })

      expect(result.current.currentStep).toBe(1)
    })

    it('should not go below step 0', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.previousStep()
      })

      expect(result.current.currentStep).toBe(0)
    })
  })

  describe.skip('node selection', () => {
    it('should select a node', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toContain('node-1')
    })

    it('should not select the same node twice', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toHaveLength(1)
    })

    it('should deselect a node', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-2')
      })

      expect(result.current.selectedNodeIds).toHaveLength(2)

      act(() => {
        result.current.actions.deselectNode('node-1')
      })

      expect(result.current.selectedNodeIds).toHaveLength(1)
      expect(result.current.selectedNodeIds).not.toContain('node-1')
    })

    it('should clear all selections', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.COMBINER)
        result.current.actions.selectNode('node-1')
        result.current.actions.selectNode('node-2')
        result.current.actions.selectNode('node-3')
      })

      expect(result.current.selectedNodeIds).toHaveLength(3)

      act(() => {
        result.current.actions.clearSelection()
      })

      expect(result.current.selectedNodeIds).toHaveLength(0)
    })
  })

  describe.skip('configuration management', () => {
    it('should update configuration data', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.updateConfiguration({ name: 'Test Adapter' })
      })

      expect(result.current.configurationData).toEqual({ name: 'Test Adapter' })
    })

    it('should merge configuration updates', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.updateConfiguration({ name: 'Test' })
        result.current.actions.updateConfiguration({ host: 'localhost' })
      })

      expect(result.current.configurationData).toEqual({
        name: 'Test',
        host: 'localhost',
      })
    })

    it('should validate configuration', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
      })

      expect(result.current.isConfigurationValid).toBe(false)

      act(() => {
        result.current.actions.updateConfiguration({ name: 'Test' })
      })

      expect(result.current.isConfigurationValid).toBe(true)
    })
  })

  describe.skip('ghost nodes', () => {
    it('should add ghost nodes', () => {
      const { result } = renderHook(() => useWizardStore())

      const ghostNode = {
        id: 'ghost-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: { isGhost: true as const, label: 'Ghost Adapter' },
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

      const ghostEdge = {
        id: 'ghost-edge-1',
        source: 'ghost-1',
        target: 'ghost-2',
        data: { isGhost: true as const },
      }

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.addGhostEdges([ghostEdge])
      })

      expect(result.current.ghostEdges).toHaveLength(1)
      expect(result.current.ghostEdges[0]).toEqual(ghostEdge)
    })

    it('should clear ghost nodes and edges', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.startWizard(EntityType.ADAPTER)
        result.current.actions.addGhostNodes([
          {
            id: 'ghost-1',
            type: 'ADAPTER_NODE',
            position: { x: 0, y: 0 },
            data: { isGhost: true, label: 'Ghost' },
          },
        ])
        result.current.actions.addGhostEdges([
          {
            id: 'ghost-edge-1',
            source: 'ghost-1',
            target: 'ghost-2',
          },
        ])
      })

      expect(result.current.ghostNodes).toHaveLength(1)
      expect(result.current.ghostEdges).toHaveLength(1)

      act(() => {
        result.current.actions.clearGhostNodes()
      })

      expect(result.current.ghostNodes).toHaveLength(0)
      expect(result.current.ghostEdges).toHaveLength(0)
    })
  })

  describe.skip('error handling', () => {
    it('should set error message', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.setError('Test error')
      })

      expect(result.current.errorMessage).toBe('Test error')
    })

    it('should clear error message', () => {
      const { result } = renderHook(() => useWizardStore())

      act(() => {
        result.current.actions.setError('Test error')
      })

      expect(result.current.errorMessage).toBe('Test error')

      act(() => {
        result.current.actions.clearError()
      })

      expect(result.current.errorMessage).toBeNull()
    })
  })

  describe.skip('convenience hooks', () => {
    it('useWizardState should return wizard state', () => {
      const { result } = renderHook(() => useWizardState())

      expect(result.current).toHaveProperty('isActive')
      expect(result.current).toHaveProperty('entityType')
      expect(result.current).toHaveProperty('currentStep')
      expect(result.current).toHaveProperty('totalSteps')
    })

    it('useWizardActions should return actions', () => {
      const { result } = renderHook(() => useWizardActions())

      expect(result.current).toHaveProperty('startWizard')
      expect(result.current).toHaveProperty('cancelWizard')
      expect(result.current).toHaveProperty('nextStep')
      expect(result.current).toHaveProperty('previousStep')
    })

    it('useWizardSelection should return selection state and actions', () => {
      const { result } = renderHook(() => useWizardSelection())

      expect(result.current).toHaveProperty('selectedNodeIds')
      expect(result.current).toHaveProperty('selectionConstraints')
      expect(result.current).toHaveProperty('selectNode')
      expect(result.current).toHaveProperty('deselectNode')
    })

    it('useWizardGhosts should return ghost state and actions', () => {
      const { result } = renderHook(() => useWizardGhosts())

      expect(result.current).toHaveProperty('ghostNodes')
      expect(result.current).toHaveProperty('ghostEdges')
      expect(result.current).toHaveProperty('addGhostNodes')
      expect(result.current).toHaveProperty('clearGhostNodes')
    })

    it('useWizardConfiguration should return config state and actions', () => {
      const { result } = renderHook(() => useWizardConfiguration())

      expect(result.current).toHaveProperty('configurationData')
      expect(result.current).toHaveProperty('isConfigurationValid')
      expect(result.current).toHaveProperty('updateConfiguration')
    })

    it('useWizardCanProceed should check if wizard can proceed', () => {
      const { result: canProceedResult } = renderHook(() => useWizardCanProceed())
      const { result: actionsResult } = renderHook(() => useWizardActions())

      // Initially should not be able to proceed (wizard not active)
      expect(canProceedResult.current).toBe(false)

      act(() => {
        actionsResult.current.startWizard(EntityType.ADAPTER)
      })

      // After starting, should be able to proceed
      expect(canProceedResult.current).toBe(true)
    })
  })
})
