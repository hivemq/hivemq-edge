import type { FC } from 'react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useDisclosure, useToast } from '@chakra-ui/react'
import { LuRefreshCw } from 'react-icons/lu'
import type { Edge, Node } from '@xyflow/react'

import IconButton from '@/components/Chakra/IconButton.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import useGetFlowElements from '@/modules/Workspace/hooks/useGetFlowElements.ts'
import { useIsPanelOpen, useReloadWorkspace } from '@/modules/Workspace/hooks/useReloadWorkspace.ts'
import ReloadWorkspaceDialog from './ReloadWorkspaceDialog.tsx'

interface ReloadWorkspaceButtonProps {
  /**
   * The graph as the builder currently derives it from the query cache — the server's picture of the
   * world once a refetch has landed, and what the canvas is reconciled against.
   *
   * Optional: with no props the button derives it itself, which is how it is used in the canvas
   * toolbar. Passing it in keeps the component testable without the whole query stack behind it.
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

  // The builder recomputes whenever the queries it reads change, so after a refetch these describe
  // the server. Props win when given, so a test can drive the reconcile with a known graph.
  const derived = useGetFlowElements()
  const nodes = builtNodes ?? derived.nodes
  const edges = builtEdges ?? derived.edges

  const handleReload = useCallback(() => {
    reload(() => {
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
  }, [reload, onReconcileWithServer, nodes, edges, toast, t, onClose])

  return (
    <>
      <IconButton
        icon={<LuRefreshCw />}
        onClick={onOpen}
        aria-label={t('workspace.reload.trigger')}
        data-testid="reload-workspace-trigger"
      />
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
