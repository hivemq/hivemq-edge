import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Icon, IconButton, Menu, MenuButton, MenuDivider, MenuGroup, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'

import type { ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'

import { HqAssets } from '@/components/Icons'
import { NodeTypes, WorkspaceNavigationCommand } from '@/modules/Workspace/types.ts'
import { NODE_ASSET_DEFAULT_ID } from '@/modules/Workspace/utils/nodes-utils.ts'

interface AssetActionMenuProps {
  asset: ManagedAsset
  onCreate?: (id: string) => void
  onEdit?: (id: string) => void
  onDelete?: (id: string) => void
  onViewWorkspace?: (id: string, type: string, command: WorkspaceNavigationCommand) => void
}

export const AssetActionMenu: FC<AssetActionMenuProps> = ({ asset, onEdit, onDelete, onViewWorkspace }) => {
  const { t } = useTranslation()

  const isUnmapped = asset.mapping === undefined || asset.mapping?.status === AssetMapping.status.UNMAPPED

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
            {t('pulse.assets.actions.view')}
          </MenuItem>
        </MenuGroup>
        <MenuGroup>
          <MenuItem data-testid="assets-action-map" isDisabled={!isUnmapped}>
            <Text>{t('pulse.assets.actions.map')}</Text>
          </MenuItem>
          <MenuItem
            data-testid="assets-action-delete"
            onClick={() => onDelete?.(asset.id)}
            sx={{
              color: 'red.500',
              _dark: { color: 'red.200' },
            }}
            isDisabled={isUnmapped}
          >
            <Text>{t('pulse.assets.actions.delete')}</Text>
          </MenuItem>
        </MenuGroup>
        <MenuDivider />

        <MenuGroup title={t('pulse.assets.actions.group.workspace')}>
          <MenuItem
            data-testid="assets-action-mapper"
            onClick={() =>
              onViewWorkspace?.(NODE_ASSET_DEFAULT_ID, NodeTypes.ASSETS_NODE, WorkspaceNavigationCommand.ASSET_MAPPER)
            }
            icon={<Icon as={HqAssets} boxSize={4} />}
            isDisabled={isUnmapped}
          >
            {t('pulse.assets.actions.mapper')}
          </MenuItem>
        </MenuGroup>
      </MenuList>
    </Menu>
  )
}
