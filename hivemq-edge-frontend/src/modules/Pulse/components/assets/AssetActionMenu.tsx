import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { IconButton, Menu, MenuButton, MenuGroup, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'

import type { ManagedAsset } from '@/api/__generated__'

interface AssetActionMenuProps {
  asset: ManagedAsset
  onCreate?: (id: string) => void
  onEdit?: (id: string) => void
  onDelete?: (id: string) => void
}

export const AssetActionMenu: FC<AssetActionMenuProps> = ({ asset, onEdit, onDelete }) => {
  const { t } = useTranslation()

  return (
    <Menu isLazy>
      <MenuButton
        variant="outline"
        size="sm"
        as={IconButton}
        icon={<ChevronDownIcon />}
        aria-label={t('pulse.assets.actions.aria-label')}
      />
      <MenuList>
        <MenuGroup>
          <MenuItem data-testid="assets-action-edit" onClick={() => onEdit?.(asset.id)}>
            {t('pulse.assets.actions.edit')}
          </MenuItem>
          <MenuItem
            data-testid="assets-action-delete"
            onClick={() => onDelete?.(asset.id)}
            sx={{
              color: 'red.500',
              _dark: { color: 'red.200' },
            }}
          >
            <Text>{t('pulse.assets.actions.delete')}</Text>
          </MenuItem>
        </MenuGroup>
      </MenuList>
    </Menu>
  )
}
