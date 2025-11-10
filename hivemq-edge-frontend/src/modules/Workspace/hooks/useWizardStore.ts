/**
 * Wizard State Management Store
 *
 * Zustand store managing the workspace wizard lifecycle and state.
 * Provides centralized state management for wizard operations including
 * step navigation, node selection, ghost previews, and configuration.
 */

import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

import type { WizardStore, WizardState, WizardType, GhostNode, GhostEdge } from '../components/wizard/types'
import { getWizardStepCount } from '../components/wizard/utils/wizardMetadata'

/**
 * Initial wizard state
 */
const initialState: WizardState = {
  isActive: false,
  entityType: null,
  currentStep: 0,
  totalSteps: 0,
  selectedNodeIds: [],
  selectionConstraints: null,
  ghostNodes: [],
  ghostEdges: [],
  configurationData: {},
  isConfigurationValid: false,
  isSidePanelOpen: false,
  errorMessage: null,
}

/**
 * Create the wizard store
 */
export const useWizardStore = create<WizardStore>()(
  devtools(
    (set, get) => ({
      ...initialState,

      actions: {
        /**
         * Start a new wizard for the given entity/integration point type
         */
        startWizard: (type: WizardType) => {
          // Get metadata from registry to determine total steps
          const totalSteps = getWizardStepCount(type)

          set({
            isActive: true,
            entityType: type,
            currentStep: 0,
            totalSteps,
            selectedNodeIds: [],
            selectionConstraints: null,
            ghostNodes: [],
            ghostEdges: [],
            configurationData: {},
            isConfigurationValid: false,
            isSidePanelOpen: false,
            errorMessage: null,
          })
        },

        /**
         * Cancel the wizard and reset all state
         */
        cancelWizard: () => {
          // Clean up ghost nodes from canvas
          // TODO: Remove ghost nodes from React Flow canvas
          // This will be handled by GhostNodeRenderer component

          set(initialState)
        },

        /**
         * Move to the next step in the wizard
         */
        nextStep: () => {
          const { currentStep, totalSteps } = get()

          if (currentStep < totalSteps - 1) {
            set({
              currentStep: currentStep + 1,
              errorMessage: null,
            })
          }
        },

        /**
         * Move to the previous step in the wizard
         */
        previousStep: () => {
          const { currentStep } = get()

          if (currentStep > 0) {
            set({
              currentStep: currentStep - 1,
              errorMessage: null,
            })
          }
        },

        /**
         * Complete the wizard and create the entity
         */
        completeWizard: async () => {
          const { entityType, actions } = get()

          if (!entityType) {
            actions.setError('No entity type selected')
            return
          }

          try {
            // TODO: Validate configuration
            if (!actions.validateConfiguration()) {
              actions.setError('Configuration validation failed')
              return
            }

            // TODO: Make API call to create entity
            // This will be implemented per entity type

            // On success:
            // 1. Remove ghost nodes
            actions.clearGhostNodes()

            // 2. Add real nodes to workspace
            // TODO: Add real nodes via workspace store

            // 3. Show success toast
            // TODO: Show success feedback

            // 4. Reset wizard
            actions.cancelWizard()
          } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
            actions.setError(errorMessage)
          }
        },

        /**
         * Select a node during interactive selection
         */
        selectNode: (nodeId: string) => {
          const { selectedNodeIds, selectionConstraints } = get()

          // Check if already selected
          if (selectedNodeIds.includes(nodeId)) {
            return
          }

          // Check max constraint
          if (selectionConstraints?.maxNodes && selectedNodeIds.length >= selectionConstraints.maxNodes) {
            return
          }

          set({
            selectedNodeIds: [...selectedNodeIds, nodeId],
          })
        },

        /**
         * Deselect a node during interactive selection
         */
        deselectNode: (nodeId: string) => {
          const { selectedNodeIds } = get()

          set({
            selectedNodeIds: selectedNodeIds.filter((id: string) => id !== nodeId),
          })
        },

        /**
         * Clear all selected nodes
         */
        clearSelection: () => {
          set({
            selectedNodeIds: [],
          })
        },

        /**
         * Update configuration data with partial updates
         */
        updateConfiguration: (data: Partial<Record<string, unknown>>) => {
          const { configurationData } = get()

          set({
            configurationData: {
              ...configurationData,
              ...data,
            },
          })

          // Revalidate after update
          get().actions.validateConfiguration()
        },

        /**
         * Validate the current configuration
         */
        validateConfiguration: () => {
          const { configurationData } = get()

          // TODO: Implement validation per entity type
          // For now, just check if we have any data
          const isValid = Object.keys(configurationData).length > 0

          set({
            isConfigurationValid: isValid,
          })

          return isValid
        },

        /**
         * Add ghost nodes to the preview
         */
        addGhostNodes: (nodes: GhostNode[]) => {
          const { ghostNodes } = get()

          set({
            ghostNodes: [...ghostNodes, ...nodes],
          })
        },

        /**
         * Add ghost edges to the preview
         */
        addGhostEdges: (edges: GhostEdge[]) => {
          const { ghostEdges } = get()

          set({
            ghostEdges: [...ghostEdges, ...edges],
          })
        },

        /**
         * Remove all ghost nodes and edges
         */
        clearGhostNodes: () => {
          set({
            ghostNodes: [],
            ghostEdges: [],
          })
        },

        /**
         * Set an error message
         */
        setError: (message: string) => {
          set({
            errorMessage: message,
          })
        },

        /**
         * Clear the current error
         */
        clearError: () => {
          set({
            errorMessage: null,
          })
        },

        /**
         * Open the side panel
         */
        openSidePanel: () => {
          set({
            isSidePanelOpen: true,
          })
        },

        /**
         * Close the side panel
         */
        closeSidePanel: () => {
          set({
            isSidePanelOpen: false,
          })
        },
      },
    }),
    {
      name: 'WizardStore',
      enabled: import.meta.env.DEV,
    }
  )
)

/**
 * Convenience hook to get wizard state
 */
export const useWizardState = () =>
  useWizardStore((state) => ({
    isActive: state.isActive,
    entityType: state.entityType,
    currentStep: state.currentStep,
    totalSteps: state.totalSteps,
    errorMessage: state.errorMessage,
  }))

/**
 * Convenience hook to get wizard actions
 */
export const useWizardActions = () => useWizardStore((state) => state.actions)

/**
 * Convenience hook to get selection state and actions
 */
export const useWizardSelection = () =>
  useWizardStore((state) => ({
    selectedNodeIds: state.selectedNodeIds,
    selectionConstraints: state.selectionConstraints,
    selectNode: state.actions.selectNode,
    deselectNode: state.actions.deselectNode,
    clearSelection: state.actions.clearSelection,
  }))

/**
 * Convenience hook to get ghost node state
 */
export const useWizardGhosts = () =>
  useWizardStore((state) => ({
    ghostNodes: state.ghostNodes,
    ghostEdges: state.ghostEdges,
    addGhostNodes: state.actions.addGhostNodes,
    addGhostEdges: state.actions.addGhostEdges,
    clearGhostNodes: state.actions.clearGhostNodes,
  }))

/**
 * Convenience hook to get configuration state and actions
 */
export const useWizardConfiguration = () =>
  useWizardStore((state) => ({
    configurationData: state.configurationData,
    isConfigurationValid: state.isConfigurationValid,
    updateConfiguration: state.actions.updateConfiguration,
    validateConfiguration: state.actions.validateConfiguration,
  }))

/**
 * Convenience hook to check if wizard can proceed to next step
 */
export const useWizardCanProceed = () =>
  useWizardStore((state) => {
    const { currentStep, totalSteps, selectedNodeIds, selectionConstraints } = state

    // Can't proceed beyond last step
    if (currentStep >= totalSteps - 1) {
      return false
    }

    // Check selection constraints if applicable
    if (selectionConstraints) {
      const minMet = !selectionConstraints.minNodes || selectedNodeIds.length >= selectionConstraints.minNodes
      const requiredMet =
        !selectionConstraints.requiredNodeIds ||
        selectionConstraints.requiredNodeIds.every((id: string) => selectedNodeIds.includes(id))

      if (!minMet || !requiredMet) {
        return false
      }
    }

    // If configuration is required, must be valid
    // TODO: Check if current step requires configuration
    // For now, always allow proceeding
    return true
  })
