import { FC } from 'react'
import { IconButton, Menu, MenuButton, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { Adapter, ConnectionStatus } from '@/api/__generated__'

interface AdapterActionMenuProps {
  adapter: Adapter
  onCreate?: (type: string | undefined) => void
  onEdit?: (id: string, type: string) => void
  onDelete?: (id: string) => void
}

const AdapterActionMenu: FC<AdapterActionMenuProps> = ({ adapter, onCreate, onEdit, onDelete }) => {
  const { t } = useTranslation()

  const { type, id, adapterRuntimeInformation: { connectionStatus } = {} } = adapter
  return (
    <Menu>
      <MenuButton
        variant="outline"
        size={'sm'}
        as={IconButton}
        icon={<ChevronDownIcon />}
        aria-label={t('protocolAdapter.table.actions.label') as string}
      />
      <MenuList>
        <MenuItem isDisabled data-testid={'adapter-action-connect'}>
          {connectionStatus?.status !== ConnectionStatus.status.CONNECTED
            ? t('protocolAdapter.table.actions.connect')
            : t('protocolAdapter.table.actions.disconnect')}
        </MenuItem>
        <MenuItem data-testid={'adapter-action-create'} onClick={() => onCreate?.(type as string)}>
          {t('protocolAdapter.table.actions.create')}
        </MenuItem>
        <MenuItem data-testid={'adapter-action-edit'} onClick={() => onEdit?.(id, type as string)}>
          {t('protocolAdapter.table.actions.edit')}
        </MenuItem>
        <MenuItem data-testid={'adapter-action-delete'} color={'red.500'} onClick={() => onDelete?.(id)}>
          <Text>{t('protocolAdapter.table.actions.delete')}</Text>
        </MenuItem>
      </MenuList>
    </Menu>
  )
}

export default AdapterActionMenu
