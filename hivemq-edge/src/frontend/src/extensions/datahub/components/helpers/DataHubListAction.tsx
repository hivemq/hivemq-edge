import { FC, MouseEventHandler } from 'react'
import { ButtonGroup } from '@chakra-ui/react'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { FaEdit } from 'react-icons/fa'
import { useTranslation } from 'react-i18next'
import { FaTrashCan } from 'react-icons/fa6'

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
        icon={<FaEdit />}
        isDisabled={isEditDisabled}
      />
      <IconButton
        data-testid="list-action-delete"
        onClick={onDelete}
        aria-label={t('Listings.action.delete')}
        icon={<FaTrashCan />}
      />
    </ButtonGroup>
  )
}

export default DataHubListAction
