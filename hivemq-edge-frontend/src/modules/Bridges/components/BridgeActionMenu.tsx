import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { IconButton, Menu, MenuButton, MenuDivider, MenuGroup, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'

import type { Bridge } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'
import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'

interface BridgeActionMenuProps {
  bridge: Bridge
  onCreate?: (id: string) => void
  onEdit?: (id: string) => void
  onDelete?: (id: string) => void
}

export const BridgeActionMenu: FC<BridgeActionMenuProps> = ({ bridge, onEdit, onDelete }) => {
  const { t } = useTranslation()

  return (
    <Menu isLazy>
      <MenuButton
        variant="outline"
        size="sm"
        as={IconButton}
        icon={<ChevronDownIcon />}
        aria-label={t('bridge.listing.column.actions')}
      />
      <MenuList>
        <ConnectionController type={DeviceTypes.BRIDGE} id={bridge.id} status={bridge.status} variant="menuItem" />
        <MenuDivider />
        <MenuGroup>
          <MenuItem data-testid="bridge-action-edit" onClick={() => onEdit?.(bridge.id)}>
            {t('bridge.action.edit')}
          </MenuItem>
          <MenuItem
            data-testid="bridge-action-delete"
            onClick={() => onDelete?.(bridge.id)}
            sx={{
              color: 'red.500',
              _dark: { color: 'red.200' },
            }}
          >
            <Text>{t('bridge.action.delete')}</Text>
          </MenuItem>
        </MenuGroup>
      </MenuList>
    </Menu>
  )
}
