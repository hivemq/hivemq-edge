import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Button } from '@chakra-ui/react'
import { LuFilePlus, LuFileEdit, LuFileQuestion } from 'react-icons/lu'

import { DesignerStatus, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const DraftCTA: FC = () => {
  const { t } = useTranslation('datahub')
  const navigate = useNavigate()
  const { setStatus, status, nodes } = useDataHubDraftStore()

  const isCleanDraft = status === DesignerStatus.DRAFT && nodes.length === 0
  const isModifiedDraft = status === DesignerStatus.DRAFT && nodes.length !== 0
  const isLoaded = status === DesignerStatus.LOADED || status === DesignerStatus.MODIFIED
  const Icon = useMemo(() => {
    if (isCleanDraft) return <LuFilePlus />
    if (isModifiedDraft) return <LuFileEdit />
    return <LuFileQuestion />
  }, [isCleanDraft, isModifiedDraft])

  const handleOnClick = () => {
    setStatus(DesignerStatus.DRAFT)
    navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
  }

  return (
    <Button leftIcon={Icon} onClick={handleOnClick} variant="primary">
      {isCleanDraft && t('Listings.policy.action.create')}
      {isModifiedDraft && t('Listings.policy.action.draft')}
      {isLoaded && t('Listings.policy.action.clear')}
    </Button>
  )
}

export default DraftCTA
