import type { FC } from 'react'
import { Button, useDisclosure } from '@chakra-ui/react'
import ConfirmationDialog from '../../../components/Modal/ConfirmationDialog'
import { useTranslation } from 'react-i18next'

interface DangerZoneProps {
  onSubmit: () => void
}

const DangerZone: FC<DangerZoneProps> = ({ onSubmit }) => {
  const { t } = useTranslation()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()

  function onHandleClear() {
    onConfirmDeleteClose()
  }

  function onHandleSubmit() {
    onConfirmDeleteClose()
    onSubmit()
  }

  return (
    <>
      <Button variant="danger" onClick={onConfirmDeleteOpen}>
        {t('combiner.actions.delete')}
      </Button>
      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={onHandleClear}
        onSubmit={onHandleSubmit}
        message={t('combiner.modal.delete.message')}
        header={t('combiner.modal.delete.header')}
        action={t('combiner.actions.delete')}
      />
    </>
  )
}

export default DangerZone
