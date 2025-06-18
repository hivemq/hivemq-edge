import type { FC, RefObject, LegacyRef } from 'react'
import { useRef } from 'react'
import {
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  Button,
  Text,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import type { FocusableElement } from '@chakra-ui/utils'

interface ConfirmationDialogProps {
  isOpen: boolean
  onClose: () => void
  header: string
  message: string
  prompt?: string
  action?: string | null
  onSubmit?: () => void
  footer?: React.ReactNode
}

const ConfirmationDialog: FC<ConfirmationDialogProps> = ({
  isOpen,
  onClose,
  header,
  message,
  prompt,
  action,
  onSubmit,
  footer,
}) => {
  const { t } = useTranslation()
  const cancelRef = useRef<HTMLButtonElement>()

  return (
    <AlertDialog
      isOpen={isOpen}
      leastDestructiveRef={cancelRef as RefObject<FocusableElement>}
      onClose={onClose}
      size="lg"
    >
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            {header}
          </AlertDialogHeader>

          <AlertDialogBody>
            <Text data-testid="confirmation-message">{message}</Text>
            {prompt && <Text data-testid="confirmation-prompt">{prompt}</Text>}
          </AlertDialogBody>

          <AlertDialogFooter gap={3}>
            <Button ref={cancelRef as LegacyRef<HTMLButtonElement>} onClick={onClose} data-testid="confirmation-cancel">
              {t('action.cancel')}
            </Button>
            {footer}
            <Button
              data-testid="confirmation-submit"
              onClick={() => {
                onClose()
                onSubmit?.()
              }}
              variant="danger"
            >
              {action || t('action.delete')}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  )
}

export default ConfirmationDialog
