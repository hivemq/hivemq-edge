import { FC } from 'react'
import { Icon, IconButton, Menu, MenuButton, MenuDivider, MenuGroup, MenuItem, MenuList, Text } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { Adapter, ProtocolAdapter } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'

import ConnectionController from '@/components/ConnectionController/ConnectionController.tsx'
import { PLCTagIcon, TopicIcon, WorkspaceIcon } from '@/components/Icons/TopicIcon.tsx'
import { deviceCapabilityIcon } from '@/modules/Workspace/utils/adapter.utils.ts'
import { WorkspaceAdapterCommand } from '@/modules/ProtocolAdapters/types.ts'

interface AdapterActionMenuProps {
  adapter: Adapter
  protocol?: ProtocolAdapter
  onCreate?: (type: string | undefined) => void
  onEdit?: (id: string, type: string) => void
  onDelete?: (id: string) => void
  onViewWorkspace?: (id: string, type: string, command: WorkspaceAdapterCommand) => void
  onExport?: (id: string, type: string) => void
}

const AdapterActionMenu: FC<AdapterActionMenuProps> = ({
  adapter,
  protocol,
  onCreate,
  onEdit,
  onDelete,
  onViewWorkspace,
  onExport,
}) => {
  const { t } = useTranslation()

  const { type, id, status } = adapter
  const { capabilities } = protocol || {}

  return (
    <Menu>
      <MenuButton
        variant="outline"
        size="sm"
        // Cannot have tooltip because of the popup menu
        as={IconButton}
        icon={<ChevronDownIcon />}
        aria-label={t('protocolAdapter.table.actions.label')}
      />
      <MenuList>
        <ConnectionController type={DeviceTypes.ADAPTER} id={id} status={status} variant="menuItem" />
        <MenuDivider />
        <MenuGroup title={t('protocolAdapter.table.actions.workspace.group')}>
          <MenuItem
            data-testid="adapter-action-tags"
            onClick={() => onViewWorkspace?.(id, type as string, WorkspaceAdapterCommand.TAGS)}
            icon={<PLCTagIcon />}
          >
            {t('protocolAdapter.table.actions.workspace.tags')}
          </MenuItem>
          <MenuItem
            data-testid="adapter-action-filters"
            onClick={() => onViewWorkspace?.(id, type as string, WorkspaceAdapterCommand.TOPIC_FILTERS)}
            icon={<Icon as={TopicIcon} />}
          >
            {t('protocolAdapter.table.actions.workspace.topicFilters')}
          </MenuItem>
          {capabilities?.includes('READ') && (
            <MenuItem
              data-testid="adapter-action-mappings-northbound"
              onClick={() => onViewWorkspace?.(id, type as string, WorkspaceAdapterCommand.MAPPINGS)}
              icon={<Icon as={deviceCapabilityIcon['READ']} />}
            >
              {t('protocolAdapter.table.actions.workspace.mappings.north')}
            </MenuItem>
          )}
          {capabilities?.includes('WRITE') && (
            <MenuItem
              data-testid="adapter-action-mappings-southbound"
              onClick={() => onViewWorkspace?.(id, type as string, WorkspaceAdapterCommand.MAPPINGS)}
              icon={<Icon as={deviceCapabilityIcon['WRITE']} />}
            >
              {t('protocolAdapter.table.actions.workspace.mappings.south')}
            </MenuItem>
          )}
          <MenuItem
            data-testid="adapter-action-workspace"
            onClick={() => onViewWorkspace?.(id, type as string, WorkspaceAdapterCommand.VIEW)}
            icon={<WorkspaceIcon />}
          >
            {t('protocolAdapter.table.actions.workspace.view')}
          </MenuItem>
        </MenuGroup>
        <MenuDivider />
        <MenuItem data-testid="adapter-action-export" onClick={() => onExport?.(id, type as string)}>
          {t('protocolAdapter.table.actions.export')}
        </MenuItem>
        <MenuDivider />
        <MenuItem data-testid="adapter-action-create" onClick={() => onCreate?.(type as string)}>
          {t('protocolAdapter.table.actions.create')}
        </MenuItem>
        <MenuItem data-testid="adapter-action-edit" onClick={() => onEdit?.(id, type as string)}>
          {t('protocolAdapter.table.actions.edit')}
        </MenuItem>
        <MenuItem
          data-testid="adapter-action-delete"
          onClick={() => onDelete?.(id)}
          sx={{
            color: 'red.500',
            _dark: { color: 'red.200' },
          }}
        >
          <Text>{t('protocolAdapter.table.actions.delete')}</Text>
        </MenuItem>
      </MenuList>
    </Menu>
  )
}

export default AdapterActionMenu
