import {
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  Button,
} from '@chakra-ui/react'
import { FocusableElement } from '@chakra-ui/utils'
import { FC, LegacyRef, RefObject, useRef } from 'react'
import { useTranslation } from 'react-i18next'

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
              colorScheme="red"
              onClick={() => {
                onClose()
                onSubmit?.()
              }}
              variant="solid"
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
