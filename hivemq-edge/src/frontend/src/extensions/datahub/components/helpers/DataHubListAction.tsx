import { FC, MouseEventHandler } from 'react'
import { ButtonGroup } from '@chakra-ui/react'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { useTranslation } from 'react-i18next'
import { LuFileEdit, LuTrash2, LuFileSearch, LuDownload } from 'react-icons/lu'
import config from '@/config'

interface DataHubListActionProps {
  onEdit?: MouseEventHandler<HTMLButtonElement>
  onDelete?: MouseEventHandler<HTMLButtonElement>
  onDownload?: MouseEventHandler<HTMLButtonElement>
  isAccessDisabled?: boolean
}

const DataHubListAction: FC<DataHubListActionProps> = ({ onEdit, onDelete, onDownload, isAccessDisabled = false }) => {
  const { t } = useTranslation('datahub')
  const isEditEnabled = config.features.DATAHUB_EDIT_POLICY_ENABLED

  return (
    <ButtonGroup size="sm" isAttached>
      {!isAccessDisabled && (
        <>
          {isEditEnabled ? (
            <IconButton
              data-testid="list-action-edit"
              onClick={onEdit}
              aria-label={t('Listings.action.edit')}
              icon={<LuFileEdit />}
            />
          ) : (
            <IconButton
              data-testid="list-action-view"
              onClick={onEdit}
              aria-label={t('Listings.action.view')}
              icon={<LuFileSearch />}
            />
          )}
        </>
      )}
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
}

export default DataHubListAction
