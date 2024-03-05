import { FC, MouseEventHandler } from 'react'
import { ButtonGroup } from '@chakra-ui/react'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { useTranslation } from 'react-i18next'
import { LuFileEdit, LuTrash2 } from 'react-icons/lu'

interface DataHubListActionProps {
  onEdit?: MouseEventHandler<HTMLButtonElement>
  onDelete?: MouseEventHandler<HTMLButtonElement>
  isEditDisabled?: boolean
}

const DataHubListAction: FC<DataHubListActionProps> = ({ onEdit, onDelete, isEditDisabled = false }) => {
  const { t } = useTranslation('datahub')
  return (
    <ButtonGroup size="sm" isAttached>
      <IconButton
        data-testid="list-action-edit"
        onClick={onEdit}
        aria-label={t('Listings.action.edit')}
        icon={<LuFileEdit />}
        isDisabled={isEditDisabled}
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
