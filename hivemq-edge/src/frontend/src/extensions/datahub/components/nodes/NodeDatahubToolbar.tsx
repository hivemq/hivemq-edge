import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonGroup, ButtonGroupProps } from '@chakra-ui/react'
import { LuCopy, LuDelete, LuFileEdit } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'

interface NodeToolbarProps extends ButtonGroupProps {
  onCopy?: (event: React.BaseSyntheticEvent) => void
  onDelete?: (event: React.BaseSyntheticEvent) => void
  onEdit?: (event: React.BaseSyntheticEvent) => void
}

const NodeDatahubToolbar: FC<NodeToolbarProps> = (props) => {
  const { t } = useTranslation('datahub')

  return (
    <ButtonGroup size="xs" variant="solid" colorScheme="gray" isAttached {...props}>
      <IconButton
        icon={<LuFileEdit />}
        data-testid="node-toolbar-edit"
        aria-label={t('Listings.action.edit')}
        onClick={props.onEdit}
      />
      <IconButton
        icon={<LuCopy />}
        data-testid="node-toolbar-copy"
        aria-label={t('Listings.action.copy')}
        onClick={props.onCopy}
      />
      <IconButton
        icon={<LuDelete />}
        data-testid="node-toolbar-delete"
        aria-label={t('Listings.action.delete')}
        colorScheme="red"
        onClick={props.onDelete}
      />
    </ButtonGroup>
  )
}

export default NodeDatahubToolbar
