import type { FC, MouseEventHandler } from 'react'
import { useTranslation } from 'react-i18next'
import { TiChevronRightOutline } from 'react-icons/ti'
import { useNavigate } from 'react-router-dom'
import { ButtonGroup, HStack, Icon } from '@chakra-ui/react'
import { LuFileEdit, LuTrash2, LuFileSearch, LuDownload } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import type { CombinedPolicy } from '@datahub/types.ts'
import { DesignerStatus, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

interface DataHubListActionProps {
  policy?: CombinedPolicy
  onEdit?: MouseEventHandler<HTMLButtonElement>
  onDelete?: MouseEventHandler<HTMLButtonElement>
  onDownload?: MouseEventHandler<HTMLButtonElement>
  onExpand?: MouseEventHandler<HTMLButtonElement>
  isExpanded?: boolean
  canExpand?: boolean
  canDelete?: boolean
  canDownload?: boolean
}

const DataHubListAction: FC<DataHubListActionProps> = ({
  policy,
  onEdit,
  onDelete,
  onDownload,
  onExpand,
  isExpanded = false,
  canExpand = false,
  canDownload = true,
  canDelete = true,
}) => {
  const { t } = useTranslation('datahub')
  const navigate = useNavigate()
  const { setStatus } = useDataHubDraftStore()

  if (!policy) {
    // If not policy, it's a resource toolbar
    return (
      <ButtonGroup size="sm" isAttached>
        {canDownload && (
          <IconButton
            data-testid="list-action-download"
            onClick={onDownload}
            aria-label={t('Listings.action.download')}
            icon={<LuDownload />}
          />
        )}
        {canDelete && (
          <IconButton
            data-testid="list-action-delete"
            onClick={onDelete}
            aria-label={t('Listings.action.delete')}
            icon={<LuTrash2 />}
          />
        )}
        {canExpand && (
          <IconButton
            data-testid="list-action-collapse"
            onClick={onExpand}
            size="sm"
            aria-label={isExpanded ? t('Listings.action.collapse') : t('Listings.action.expand')}
            icon={
              <Icon as={TiChevronRightOutline} fontSize="1.5rem" transform={isExpanded ? 'rotate(90deg)' : undefined} />
            }
          />
        )}
      </ButtonGroup>
    )
  }

  if (policy?.type === PolicyType.CREATE_POLICY) {
    // If a draft
    return (
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
  }

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
