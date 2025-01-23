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
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import type { FocusableElement } from '@chakra-ui/utils'

interface ConfirmationDialogProps {
  isOpen: boolean
  onClose: () => void
  header: string
  message: string
  action?: string | null
  onSubmit?: () => void
}

const ConfirmationDialog: FC<ConfirmationDialogProps> = ({ isOpen, onClose, header, message, action, onSubmit }) => {
  const { t } = useTranslation()
  const cancelRef = useRef<HTMLButtonElement>()

  return (
    <AlertDialog isOpen={isOpen} leastDestructiveRef={cancelRef as RefObject<FocusableElement>} onClose={onClose}>
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            {header}
          </AlertDialogHeader>

          <AlertDialogBody>{message}</AlertDialogBody>

          <AlertDialogFooter>
            <Button ref={cancelRef as LegacyRef<HTMLButtonElement>} onClick={onClose} data-testid="confirmation-cancel">
              {t('action.cancel')}
            </Button>
            <Button
              data-testid="confirmation-submit"
              onClick={() => {
                onClose()
                onSubmit?.()
              }}
              variant="danger"
              ml={3}
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
