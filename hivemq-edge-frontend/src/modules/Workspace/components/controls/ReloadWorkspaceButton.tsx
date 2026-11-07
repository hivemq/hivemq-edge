import type { FC } from 'react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, useDisclosure, useToast } from '@chakra-ui/react'
import { LuRefreshCw } from 'react-icons/lu'
import type { Edge, Node } from '@xyflow/react'

import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { useFlowElementsBuilder } from '@/modules/Workspace/hooks/useGetFlowElements.ts'
import { useIsPanelOpen, useReloadWorkspace } from '@/modules/Workspace/hooks/useReloadWorkspace.ts'
import ReloadWorkspaceDialog from './ReloadWorkspaceDialog.tsx'

interface ReloadWorkspaceButtonProps {
  /**
   * The graph to reconcile the canvas against, standing in for the server's picture of the world.
   *
   * Optional: with no props the button builds it from the lists the reload has just fetched, which is
   * how it is used in the canvas toolbar. Passing it in keeps the component testable without the
   * whole query stack behind it.
   */
  builtNodes?: Node[]
  builtEdges?: Edge[]
}

/**
 * The prominent entry point for repairing a workspace that has drifted from the server.
 *
 * It sits in the main toolbar rather than behind the options gear, because discoverability is half
 * the bug: the reset that exists today is two levels down behind an icon nobody recognises, so users
 * who suspect the view is wrong never find it and end up clearing browser storage by hand.
 */
const ReloadWorkspaceButton: FC<ReloadWorkspaceButtonProps> = ({ builtNodes, builtEdges }) => {
  const { t } = useTranslation()
  const toast = useToast()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const isPanelOpen = useIsPanelOpen()
  const { isReloading, reload, resetEverything } = useReloadWorkspace()
  const onReconcileWithServer = useWorkspaceStore((state) => state.onReconcileWithServer)
  const buildFlowElements = useFlowElementsBuilder()

  const handleReload = useCallback(() => {
    reload((fetched) => {
      // Built from what this reload fetched: a graph read from render state predates the fetch and still
      // holds whatever was deleted since the canvas loaded. Props win, so a test can drive a known graph.
      const built = buildFlowElements(fetched)
      const nodes = builtNodes ?? built.nodes
      const edges = builtEdges ?? built.edges

      // Reconciling merges the server's picture into the canvas in place, keeping the user's layout.
      const summary = onReconcileWithServer(nodes, edges)

      toast({
        status: 'success',
        title: t('workspace.reload.success', summary),
        isClosable: true,
      })
    })
      .catch(() => {
        // All-or-nothing: a failed fetch leaves the workspace exactly as it was.
        toast({ status: 'error', title: t('workspace.reload.error'), isClosable: true })
      })
      .finally(onClose)
  }, [reload, buildFlowElements, builtNodes, builtEdges, onReconcileWithServer, toast, t, onClose])

  return (
    <>
      {/*
        A labelled button, not an icon in the control group: the point of this control is that a
        confused user finds it, and the five icon-only controls beside it are exactly the pattern
        that made the old reset undiscoverable.
      */}
      <Button
        leftIcon={<LuRefreshCw />}
        onClick={onOpen}
        size="sm"
        variant="outline"
        backgroundColor="white"
        data-testid="reload-workspace-trigger"
      >
        {t('workspace.reload.trigger')}
      </Button>
      <ReloadWorkspaceDialog
        isOpen={isOpen}
        onClose={onClose}
        isReloading={isReloading}
        isPanelOpen={isPanelOpen}
        onReload={handleReload}
        onResetEverything={resetEverything}
      />
    </>
  )
}

export default ReloadWorkspaceButton
