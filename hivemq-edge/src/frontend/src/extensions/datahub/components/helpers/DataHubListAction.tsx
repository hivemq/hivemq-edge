import { FC, MouseEventHandler } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { ButtonGroup, HStack } from '@chakra-ui/react'
import { LuFileEdit, LuTrash2, LuFileSearch, LuDownload } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { CombinedPolicy, DesignerStatus, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

interface DataHubListActionProps {
  policy?: CombinedPolicy
  onEdit?: MouseEventHandler<HTMLButtonElement>
  onDelete?: MouseEventHandler<HTMLButtonElement>
  onDownload?: MouseEventHandler<HTMLButtonElement>
}

const DataHubListAction: FC<DataHubListActionProps> = ({ policy, onEdit, onDelete, onDownload }) => {
  const { t } = useTranslation('datahub')
  const navigate = useNavigate()
  const { setStatus } = useDataHubDraftStore()

  const renderResourceToolbar = () => (
    <ButtonGroup size="sm" isAttached>
      <IconButton
        data-testid="list-action-download"
        onClick={onDownload}
        aria-label={t('Listings.action.download')}
        icon={<LuDownload />}
      />
      <IconButton
        data-testid="list-action-delete"
        onClick={onDelete}
        aria-label={t('Listings.action.delete')}
        icon={<LuTrash2 />}
      />
    </ButtonGroup>
  )

  const renderDraftToolbar = () => (
    <ButtonGroup size="sm" isAttached>
      <IconButton
        data-testid="list-action-view"
        onClick={() => {
          setStatus(DesignerStatus.DRAFT)
          navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
        }}
        aria-label={t('Listings.policy.action.draft')}
        icon={<LuFileEdit />}
      />
      <IconButton
        data-testid="list-action-delete"
        onClick={onDelete}
        aria-label={t('Listings.action.delete')}
        icon={<LuTrash2 />}
      />
    </ButtonGroup>
  )

  if (!policy) return renderResourceToolbar()
  if (policy?.type === PolicyType.CREATE_POLICY) return renderDraftToolbar()
  return (
    <HStack>
      <ButtonGroup size="sm" isAttached>
        <IconButton
          data-testid="list-action-view"
          onClick={onEdit}
          aria-label={t('Listings.action.view-edit')}
          icon={<LuFileSearch />}
        />
        <IconButton
          data-testid="list-action-download"
          onClick={onDownload}
          aria-label={t('Listings.action.download')}
          icon={<LuDownload />}
        />
        <IconButton
          data-testid="list-action-delete"
          onClick={onDelete}
          aria-label={t('Listings.action.delete')}
          icon={<LuTrash2 />}
        />
      </ButtonGroup>
    </HStack>
  )
}

export default DataHubListAction
