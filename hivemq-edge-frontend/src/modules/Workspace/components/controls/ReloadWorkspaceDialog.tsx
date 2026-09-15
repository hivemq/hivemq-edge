import type { FC } from 'react'
import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import {
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  Alert,
  AlertIcon,
  Button,
  Box,
  Center,
  Spinner,
  Text,
  VStack,
} from '@chakra-ui/react'

interface ReloadWorkspaceDialogProps {
  isOpen: boolean
  onClose: () => void
  /** True while a reload is running — the dialog turns into a blocking progress state. */
  isReloading: boolean
  /** True when a configuration panel is open, in which case neither choice is offered. */
  isPanelOpen: boolean
  onReload: () => void
  onResetEverything: () => void
}

/**
 * What the dialog says, which is one of three things: it is working, it cannot work yet because a
 * panel is open, or it is offering the two choices. Kept apart from the dialog frame so the three
 * states read as three states rather than as a chain of conditions.
 */
const ReloadDialogBody: FC<Pick<ReloadWorkspaceDialogProps, 'isReloading' | 'isPanelOpen'>> = ({
  isReloading,
  isPanelOpen,
}) => {
  const { t } = useTranslation()

  if (isReloading) {
    return (
      <Center flexDirection="column" gap={4} py={8} data-testid="reload-workspace-progress">
        <Spinner size="lg" thickness="3px" speed="0.8s" />
        <Text aria-live="polite">{t('workspace.reload.progress')}</Text>
      </Center>
    )
  }

  if (isPanelOpen) {
    return (
      <Alert status="warning" data-testid="reload-workspace-panel-warning">
        <AlertIcon />
        <Text>{t('workspace.reload.panelOpen')}</Text>
      </Alert>
    )
  }

  return (
    <VStack align="stretch" spacing={5}>
      <Box>
        <Text fontWeight="semibold">{t('workspace.reload.safe.title')}</Text>
        <Text fontSize="sm">{t('workspace.reload.safe.description')}</Text>
      </Box>
      <Box>
        <Text fontWeight="semibold">{t('workspace.reload.destructive.title')}</Text>
        <Text fontSize="sm">{t('workspace.reload.destructive.description')}</Text>
      </Box>
    </VStack>
  )
}

/**
 * The two choices behind the Reload button.
 *
 * One entry point rather than two buttons: a user who suspects the workspace is wrong should not have
 * to know *which* kind of reload they need before they can find either. The safe choice is the easy
 * one; the destructive choice is present but deliberately harder to reach for.
 *
 * While a reload runs the dialog stays open and shows a spinner, which is what blocks the user out of
 * the canvas — no clicking, dragging or navigating into a half-reconciled graph.
 */
const ReloadWorkspaceDialog: FC<ReloadWorkspaceDialogProps> = ({
  isOpen,
  onClose,
  isReloading,
  isPanelOpen,
  onReload,
  onResetEverything,
}) => {
  const { t } = useTranslation()
  const cancelRef = useRef<HTMLButtonElement>(null)

  return (
    <AlertDialog
      isOpen={isOpen}
      leastDestructiveRef={cancelRef}
      // A reload in flight must not be dismissed halfway through.
      onClose={isReloading ? () => undefined : onClose}
      closeOnOverlayClick={!isReloading}
      closeOnEsc={!isReloading}
      size="xl"
    >
      <AlertDialogOverlay>
        <AlertDialogContent data-testid="reload-workspace-dialog">
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            {t('workspace.reload.header')}
          </AlertDialogHeader>

          <AlertDialogBody>
            <ReloadDialogBody isReloading={isReloading} isPanelOpen={isPanelOpen} />
          </AlertDialogBody>

          <AlertDialogFooter gap={3}>
            <Button ref={cancelRef} onClick={onClose} isDisabled={isReloading} data-testid="reload-workspace-cancel">
              {t('workspace.reload.action.cancel')}
            </Button>
            {!isPanelOpen && (
              <>
                <Button
                  variant="danger"
                  onClick={onResetEverything}
                  isDisabled={isReloading}
                  data-testid="reload-workspace-reset"
                >
                  {t('workspace.reload.action.reset')}
                </Button>
                <Button
                  variant="primary"
                  onClick={onReload}
                  isLoading={isReloading}
                  data-testid="reload-workspace-confirm"
                >
                  {t('workspace.reload.action.reload')}
                </Button>
              </>
            )}
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  )
}

export default ReloadWorkspaceDialog
