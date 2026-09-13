import type { FC } from 'react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, useDisclosure, useToast } from '@chakra-ui/react'
import { LuRefreshCw } from 'react-icons/lu'
import type { Edge, Node } from '@xyflow/react'

import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { useIsPanelOpen, useReloadWorkspace } from '@/modules/Workspace/hooks/useReloadWorkspace.ts'
import ReloadWorkspaceDialog from './ReloadWorkspaceDialog.tsx'

interface ReloadWorkspaceButtonProps {
  /**
   * The graph as the builder currently derives it from the query cache. After a refetch this is the
   * server's picture of the world, and what the canvas is reconciled against.
   */
  builtNodes: Node[]
  builtEdges: Edge[]
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

  const handleReload = useCallback(() => {
    reload(() => {
      // The builder recomputes from the refreshed query cache, so by the time this runs the arrays
      // it produced describe the server. Reconciling merges that into the canvas in place.
      const summary = onReconcileWithServer(builtNodes, builtEdges)

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
  }, [reload, onReconcileWithServer, builtNodes, builtEdges, toast, t, onClose])

  return (
    <>
      <Button
        leftIcon={<LuRefreshCw />}
        onClick={onOpen}
        size="sm"
        variant="outline"
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
