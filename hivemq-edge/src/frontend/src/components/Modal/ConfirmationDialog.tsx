import { FC, RefObject, useRef, LegacyRef } from 'react'
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
import { FocusableElement } from '@chakra-ui/utils'

interface ConfirmationDialogProps {
  isOpen: boolean
  onClose: () => void
  header: string
  message: string
  onSubmit?: () => void
}

const ConfirmationDialog: FC<ConfirmationDialogProps> = ({ isOpen, onClose, header, message, onSubmit }) => {
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
            <Button ref={cancelRef as LegacyRef<HTMLButtonElement>} onClick={onClose}>
              {t('action.cancel')}
            </Button>
            <Button
              onClick={() => {
                onClose()
                onSubmit?.()
              }}
              variant="danger"
              ml={3}
            >
              {t('action.delete')}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  )
}

export default ConfirmationDialog
