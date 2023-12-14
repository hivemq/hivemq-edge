import { ChevronDownIcon } from '@chakra-ui/icons'
import { IconButton, Menu, MenuButton, MenuDivider, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import { Adapter } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'

import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'

interface AdapterActionMenuProps {
  adapter: Adapter
  onCreate?: (type: string | undefined) => void
  onEdit?: (id: string, type: string) => void
  onDelete?: (id: string) => void
  onViewWorkspace?: (id: string, type: string) => void
}

const AdapterActionMenu: FC<AdapterActionMenuProps> = ({ adapter, onCreate, onEdit, onDelete, onViewWorkspace }) => {
  const { t } = useTranslation()

  const { type, id, status } = adapter
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
        <ConnectionController type={DeviceTypes.ADAPTER} id={id} status={status} variant={'menuItem'} />

        <MenuItem data-testid={'adapter-action-workspace'} onClick={() => onViewWorkspace?.(id, type as string)}>
          {t('protocolAdapter.table.actions.workspace')}
        </MenuItem>
        <MenuDivider />
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
