import { useReactFlow } from '@xyflow/react'
import { removeGhostEdges, removeGhostNodes } from '@/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore.ts'
import { GHOST_SUCCESS_OPACITY_TRANSITION } from '../utils/styles'

export const useCompleteUtilities = () => {
  const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()

  // TRANSITION SEQUENCE: Ghost → Real nodes
  // ==========================================
  // Current implementation uses time-based delays to create a smooth visual transition.
  // This works well but could be improved with event-based triggers.
  //
  // CURRENT APPROACH:
  // - API completes → Ghost fades out (500ms) → Wait 600ms → Remove ghost → Real nodes appear
  // - Total perceived transition: ~600ms
  //
  // POTENTIAL IMPROVEMENTS:
  // - Listen to React Query cache update event instead of fixed delay
  // - Wait for actual DOM render of real nodes before removing ghosts
  // - Use React Flow's onNodesChange to detect new nodes appearing
  // - Coordinate with useGetFlowElements refresh cycle
  // - Add loading skeleton/shimmer during transition
  // - Morph ghost directly into real node (position already matches)
  //
  // CONSTRAINTS TO CONSIDER:
  // - React Query invalidation timing (when does refetch complete?)
  // - useGetFlowElements useEffect dependencies and execution order
  // - React Flow render cycle (when are new nodes actually painted?)
  // - Browser paint timing (requestAnimationFrame considerations)
  // - User perception (anything under 300ms feels instant, 300-1000ms needs feedback)
  //
  // For now, the time-based approach provides a reliable, smooth transition.
  // Future iteration could make this more robust with proper event coordination.
  const handleTransitionSequence = async () => {
    const nodes = getNodes()

    // Fade out ghost nodes (blue glow dims to 30% opacity over 500ms)
    const nodesWithFade = nodes.map((node) => {
      if (node.data?.isGhost) {
        return {
          ...node,
          style: {
            ...node.style,
            opacity: 0.3,
            transition: GHOST_SUCCESS_OPACITY_TRANSITION,
          },
        }
      }
      return node
    })

    setNodes(nodesWithFade)

    // 3. Wait for fade animation to complete and real nodes to appear
    // The 600ms delay allows:
    // - Ghost fade animation to complete (500ms)
    // - React Query to invalidate and refetch (variable)
    // - useGetFlowElements to process new data (variable)
    // - React Flow to render new nodes (variable)
    // TODO: Replace with event-based trigger when React Query cache updates
    await new Promise((resolve) => setTimeout(resolve, 600))

    // 4. Remove ghost nodes and edges
    // By this point, real nodes should be visible at the same position
    const realNodes = removeGhostNodes(getNodes())
    const realEdges = removeGhostEdges(getEdges())

    setNodes(realNodes)
    setEdges(realEdges)

    // 5. Reset wizard state (we handle API and validation in this hook)
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()
  }

  return { handleTransitionSequence }
}
