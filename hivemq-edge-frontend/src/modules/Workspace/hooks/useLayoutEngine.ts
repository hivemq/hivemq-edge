/**
 * Layout Engine Hook
 *
 * Main hook for applying layout algorithms to the workspace.
 * Orchestrates layout calculation, constraint handling, and state updates.
 */

import { useCallback, useMemo } from 'react'
import { useReactFlow } from '@xyflow/react'
import useWorkspaceStore from './useWorkspaceStore'
import { layoutRegistry } from '../utils/layout/layout-registry'
import { extractLayoutConstraints } from '../utils/layout/constraint-utils'
import type { LayoutType, LayoutOptions, LayoutResult } from '../types/layout'

/**
 * Hook for managing and applying layout algorithms
 *
 * Provides methods to apply layouts, manage layout configuration,
 * and handle layout presets and history.
 *
 * @example
 * ```typescript
 * const { applyLayout, currentAlgorithm, setAlgorithm } = useLayoutEngine()
 *
 * // Change algorithm
 * setAlgorithm(LayoutType.DAGRE_LR)
 *
 * // Apply layout
 * await applyLayout()
 * ```
 */
export const useLayoutEngine = () => {
  const reactFlowInstance = useReactFlow()
  const {
    nodes,
    edges,
    onNodesChange,
    layoutConfig,
    setLayoutAlgorithm,
    setLayoutMode,
    setLayoutOptions,
    isAutoLayoutEnabled,
    toggleAutoLayout,
    saveLayoutPreset,
    loadLayoutPreset,
    deleteLayoutPreset,
    pushLayoutHistory,
    clearLayoutHistory,
    layoutHistory,
  } = useWorkspaceStore()

  /**
   * Get current layout algorithm instance
   */
  const currentAlgorithm = useMemo(
    () => layoutRegistry.get(layoutConfig.currentAlgorithm),
    [layoutConfig.currentAlgorithm]
  )

  /**
   * Apply the current layout algorithm to the workspace
   *
   * @returns Promise resolving to layout result
   */
  const applyLayout = useCallback(async (): Promise<LayoutResult | null> => {
    // ✅ FIX: Read fresh layoutConfig from store, not from closure!
    const freshLayoutConfig = useWorkspaceStore.getState().layoutConfig
    const freshAlgorithm = layoutRegistry.get(freshLayoutConfig.currentAlgorithm)

    if (!freshAlgorithm) {
      console.warn('No layout algorithm selected')
      return null
    }

    if (nodes.length === 0) {
      console.warn('No nodes to layout')
      return null
    }

    console.log(`Applying ${freshAlgorithm.name} to ${nodes.length} nodes...`)
    console.log('📊 Current layout options:', freshLayoutConfig.options)

    // Extract constraints from current node structure
    const constraints = extractLayoutConstraints(nodes, edges)

    // Validate options before applying
    const validation = freshAlgorithm.validateOptions(freshLayoutConfig.options)
    if (!validation.valid) {
      console.error('Layout options validation failed:', validation.errors)
      return {
        nodes,
        duration: 0,
        success: false,
        error: `Invalid options: ${validation.errors?.join(', ')}`,
      }
    }

    if (validation.warnings && validation.warnings.length > 0) {
      console.warn('Layout warnings:', validation.warnings)
    }

    try {
      // Apply layout algorithm
      const result = await freshAlgorithm.apply(nodes, edges, freshLayoutConfig.options, constraints)

      if (result.success) {
        const animationDuration = freshLayoutConfig.options.animate
          ? freshLayoutConfig.options.animationDuration || 300
          : 0

        if (animationDuration > 0) {
          // Step 1: Add transition styles to all nodes
          const currentNodes = reactFlowInstance.getNodes()
          const addStyleChanges = currentNodes.map((node) => ({
            id: node.id,
            type: 'replace' as const,
            item: {
              ...node,
              style: {
                ...node.style,
                transition: `transform ${animationDuration}ms cubic-bezier(0.4, 0, 0.2, 1)`,
              },
            },
          }))
          onNodesChange(addStyleChanges)

          // Step 2: Wait for next frame, then update positions (triggers animation)
          requestAnimationFrame(() => {
            const changes = result.nodes.map((node) => ({
              id: node.id,
              type: 'position' as const,
              position: node.position,
              positionAbsolute: node.position,
              ...(node.sourcePosition && { sourcePosition: node.sourcePosition }),
              ...(node.targetPosition && { targetPosition: node.targetPosition }),
            }))
            onNodesChange(changes)

            // Step 3: Remove transition styles after animation completes
            setTimeout(() => {
              const finalNodes = reactFlowInstance.getNodes()
              const removeStyleChanges = finalNodes.map((node) => {
                const { transition, ...restStyle } = node.style || {}
                return {
                  id: node.id,
                  type: 'replace' as const,
                  item: {
                    ...node,
                    style: restStyle,
                  },
                }
              })
              onNodesChange(removeStyleChanges)
            }, animationDuration)
          })
        } else {
          // No animation - just update positions immediately
          const changes = result.nodes.map((node) => ({
            id: node.id,
            type: 'position' as const,
            position: node.position,
            positionAbsolute: node.position,
            ...(node.sourcePosition && { sourcePosition: node.sourcePosition }),
            ...(node.targetPosition && { targetPosition: node.targetPosition }),
          }))
          onNodesChange(changes)
        }

        // Save to history
        pushLayoutHistory({
          id: crypto.randomUUID(),
          timestamp: new Date(),
          algorithm: freshLayoutConfig.currentAlgorithm,
          options: freshLayoutConfig.options,
          nodePositions: new Map(result.nodes.map((n) => [n.id, n.position])),
        })

        // Fit view if requested
        if (freshLayoutConfig.options.fitView) {
          setTimeout(() => {
            reactFlowInstance.fitView({
              padding: freshLayoutConfig.options.fitViewOptions?.padding || 0.2,
              includeHiddenNodes: freshLayoutConfig.options.fitViewOptions?.includeHiddenNodes || false,
              duration: freshLayoutConfig.options.animate ? freshLayoutConfig.options.animationDuration || 300 : 0,
            })
          }, 50)
        }

        console.log(`✓ Layout applied successfully in ${result.duration.toFixed(2)}ms`)
        console.log(`  - Algorithm: ${freshAlgorithm.name}`)
        console.log(`  - Nodes: ${result.metadata?.nodeCount || nodes.length}`)
        console.log(`  - Edges: ${result.metadata?.edgeCount || edges.length}`)
      } else {
        console.error('✗ Layout failed:', result.error)
      }

      return result
    } catch (error) {
      console.error('Layout engine error:', error)
      return {
        nodes,
        duration: 0,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error in layout engine',
      }
    }
  }, [nodes, edges, onNodesChange, pushLayoutHistory, reactFlowInstance])

  /**
   * Apply a specific layout algorithm with custom options
   *
   * @param type - Layout algorithm type
   * @param options - Optional custom options (merged with current)
   * @returns Promise resolving to layout result
   */
  const applyLayoutWithAlgorithm = useCallback(
    async (type: LayoutType, options?: Partial<LayoutOptions>): Promise<LayoutResult | null> => {
      const previousAlgorithm = layoutConfig.currentAlgorithm
      const previousOptions = layoutConfig.options

      // Temporarily set algorithm and options
      setLayoutAlgorithm(type)
      if (options) {
        setLayoutOptions(options)
      }

      // Apply layout
      const result = await applyLayout()

      // Restore if failed
      if (result && !result.success) {
        setLayoutAlgorithm(previousAlgorithm)
        if (options) {
          setLayoutOptions(previousOptions)
        }
      }

      return result
    },
    [layoutConfig, setLayoutAlgorithm, setLayoutOptions, applyLayout]
  )

  /**
   * Save current node positions as a preset
   *
   * @param name - Name for the preset
   * @param description - Optional description
   */
  const saveCurrentLayout = useCallback(
    (name: string, description?: string) => {
      const preset = {
        id: crypto.randomUUID(),
        name,
        description,
        algorithm: layoutConfig.currentAlgorithm,
        options: layoutConfig.options,
        positions: new Map(nodes.map((node) => [node.id, node.position])),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      saveLayoutPreset(preset)
      console.log(`✓ Layout preset "${name}" saved`)
    },
    [layoutConfig, nodes, saveLayoutPreset]
  )

  /**
   * Load a preset and apply it
   *
   * @param presetId - ID of preset to load
   */
  const loadPreset = useCallback(
    async (presetId: string) => {
      loadLayoutPreset(presetId)
      // Apply layout after loading preset
      setTimeout(() => applyLayout(), 50)
    },
    [loadLayoutPreset, applyLayout]
  )

  /**
   * Undo last layout change
   */
  const undo = useCallback(() => {
    if (layoutHistory.length < 2) {
      console.warn('No layout history to undo')
      return
    }

    // Get second-to-last entry (last is current state)
    const previousEntry = layoutHistory.at(-2)
    if (!previousEntry) return

    // Apply previous positions
    const changes = Array.from(previousEntry.nodePositions.entries()).map(([id, position]) => ({
      id,
      type: 'position' as const,
      position,
      positionAbsolute: position,
    }))

    onNodesChange(changes)

    // Remove last entry from history
    clearLayoutHistory()
    for (const entry of layoutHistory.slice(0, -1)) {
      pushLayoutHistory(entry)
    }

    console.log('✓ Layout undo successful')
  }, [layoutHistory, onNodesChange, clearLayoutHistory, pushLayoutHistory])

  /**
   * Reset layout options to defaults
   */
  const resetOptionsToDefault = useCallback(() => {
    if (currentAlgorithm) {
      setLayoutOptions(currentAlgorithm.defaultOptions)
      console.log('✓ Layout options reset to defaults')
    }
  }, [currentAlgorithm, setLayoutOptions])

  return {
    // Core layout operations
    applyLayout,
    applyLayoutWithAlgorithm,

    // Algorithm selection
    currentAlgorithm: layoutConfig.currentAlgorithm,
    currentAlgorithmInstance: currentAlgorithm,
    setAlgorithm: setLayoutAlgorithm,
    availableAlgorithms: layoutRegistry.getAll(),

    // Mode control
    layoutMode: layoutConfig.mode,
    setLayoutMode,
    isAutoLayoutEnabled,
    toggleAutoLayout,

    // Options management
    layoutOptions: layoutConfig.options,
    setLayoutOptions,
    resetOptionsToDefault,

    // Preset management
    presets: layoutConfig.presets,
    saveCurrentLayout,
    loadPreset,
    deletePreset: deleteLayoutPreset,

    // History management
    canUndo: layoutHistory.length > 1,
    undo,
    layoutHistory,
    clearHistory: clearLayoutHistory,

    // Registry access
    layoutRegistry,
  }
}
