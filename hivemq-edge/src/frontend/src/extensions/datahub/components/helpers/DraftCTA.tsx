import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Button } from '@chakra-ui/react'
import { LuFilePlus } from 'react-icons/lu'

import { DesignerStatus, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const DraftCTA: FC = () => {
  const { t } = useTranslation('datahub')
  const navigate = useNavigate()
  const { setStatus, reset } = useDataHubDraftStore()

  const handleOnClick = () => {
    reset()
    setStatus(DesignerStatus.DRAFT)
    navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
  }

  return (
    <Button leftIcon={<LuFilePlus />} onClick={handleOnClick} variant="primary">
      {t('Listings.policy.action.create')}
    </Button>
  )
}

export default DraftCTA
