import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Button, useDisclosure } from '@chakra-ui/react'
import { LuFilePlus } from 'react-icons/lu'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import { DesignerStatus, DesignerPolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const DraftCTA: FC = () => {
  const { t } = useTranslation('datahub')
  const navigate = useNavigate()
  const { setStatus, reset, isDirty } = useDataHubDraftStore()
  const { isOpen, onOpen, onClose } = useDisclosure()

  const handleOnClick = () => {
    if (isDirty()) onOpen()
    else navigate(`/datahub/${DesignerPolicyType.CREATE_POLICY}`)
  }

  const handleReplaceDraft = () => {
    onClose()
    reset()
    setStatus(DesignerStatus.DRAFT)
    navigate(`/datahub/${DesignerPolicyType.CREATE_POLICY}`)
  }

  const handleGoDraft = () => {
    onClose()
    navigate(`/datahub/${DesignerPolicyType.CREATE_POLICY}`)
  }

  return (
    <>
      <Button leftIcon={<LuFilePlus />} onClick={handleOnClick} variant="primary">
        {t('Listings.policy.action.create')}
      </Button>
      <ConfirmationDialog
        isOpen={isOpen}
        onClose={onClose}
        onSubmit={handleReplaceDraft}
        message={t('workspace.toolbars.modal.replace.confirmation')}
        header={t('workspace.toolbars.modal.replace.header')}
        action={t('workspace.controls.replace')}
        footer={
          <Button data-testid="confirmation-navigate-draft" onClick={handleGoDraft}>
            {t('workspace.controls.goDraft')}
          </Button>
        }
      />
    </>
  )
}

export default DraftCTA
