import type { FC } from 'react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Icon, IconButton, Menu, MenuButton, MenuDivider, MenuGroup, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'
import debug from 'debug'

import type { ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { useListAssetMappers } from '@/api/hooks/useAssetMapper'

import { HqAssets } from '@/components/Icons'
import { NodeTypes, WorkspaceNavigationCommand } from '@/modules/Workspace/types.ts'

interface AssetActionMenuProps {
  asset: ManagedAsset
  isInWorkspace?: boolean
  onCreate?: (id: string) => void
  onEdit?: (id: string) => void
  onView?: (id: string) => void
  onDelete?: (id: string) => void
  onViewWorkspace?: (id: string, type: string, command: WorkspaceNavigationCommand) => void
}

const combinerLog = debug(`Combiner:AssetActionMenu`)

export const AssetActionMenu: FC<AssetActionMenuProps> = ({
  asset,
  onEdit,
  onDelete,
  onView,
  onViewWorkspace,
  isInWorkspace = false,
}) => {
  const { t } = useTranslation()
  const { data: listMappers } = useListAssetMappers()

  const getMapper = useCallback(
    (assetId: string | undefined) => {
      if (!assetId) return undefined

      return listMappers?.items.find((mapper) =>
        mapper.mappings?.items.some((mapping) => {
          return mapping.destination.assetId === assetId
        })
      )
    },
    [listMappers?.items]
  )

  const isUnmapped = asset.mapping.status === AssetMapping.status.UNMAPPED

  return (
    <Menu isLazy id="asset-actions">
      <MenuButton
        variant="outline"
        size="sm"
        as={IconButton}
        icon={<ChevronDownIcon />}
        aria-label={t('pulse.assets.actions.aria-label')}
      />
      <MenuList>
        <MenuGroup>
          <MenuItem data-testid="assets-action-view" onClick={() => onView?.(asset.id)}>
            {t('pulse.assets.actions.view')}
          </MenuItem>
        </MenuGroup>
        <MenuGroup>
          <MenuItem data-testid="assets-action-map" isDisabled={!isUnmapped} onClick={() => onEdit?.(asset.id)}>
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

        {!isInWorkspace && (
          <>
            <MenuDivider />
            <MenuGroup title={t('pulse.assets.actions.group.workspace')}>
              <MenuItem
                data-testid="assets-action-mapper"
                onClick={() => {
                  const mapper = getMapper(asset.mapping.mappingId)
                  if (!mapper) {
                    combinerLog('Cannot find the mapper')
                    return
                  }
                  return onViewWorkspace?.(mapper.id, NodeTypes.COMBINER_NODE, WorkspaceNavigationCommand.ASSET_MAPPER)
                }}
                icon={<Icon as={HqAssets} boxSize={4} />}
                isDisabled={isUnmapped}
              >
                {t('pulse.assets.actions.mapper')}
              </MenuItem>
            </MenuGroup>
          </>
        )}
      </MenuList>
    </Menu>
  )
}
