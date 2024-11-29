import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, Card, Text } from '@chakra-ui/react'
import { LuPencil, LuPlus, LuTrash } from 'react-icons/lu'

import { FieldMappingsModel } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { PLCTag, Topic } from '@/components/MQTT/EntityTag.tsx'

interface ListSubscriptionsProps {
  items: FieldMappingsModel[]
  onEdit?: (index: number) => void
  onDelete?: (index: number) => void
  onAdd?: () => void
}

const ListMappings: FC<ListSubscriptionsProps> = ({ items, onEdit, onDelete, onAdd }) => {
  const { t } = useTranslation('components')

  const columns = useMemo<ColumnDef<FieldMappingsModel>[]>(() => {
    return [
      {
        accessorKey: 'topicFilter',
        enableSorting: false,
        cell: (info) => {
          const values = info.getValue<string>()
          if (!values?.length) return <Text>{t('rjsf.MqttTransformationField.unset')}</Text>
          return <Topic key={values} tagTitle={values} />
        },
        header: t('rjsf.MqttTransformationField.listing.sources'),
      },
      {
        accessorKey: 'tag',
        cell: (info) => {
          const val = info.getValue<string>()
          return val ? <PLCTag tagTitle={val} /> : <Text>{t('rjsf.MqttTransformationField.unset')}</Text>
        },
        header: t('rjsf.MqttTransformationField.listing.destination'),
        enableSorting: false,
      },
      {
        id: 'actions',
        header: t('rjsf.MqttTransformationField.listing.actions'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup isAttached size="xs">
              <IconButton
                aria-label={t('rjsf.MqttTransformationField.actions.edit.aria-label')}
                icon={<LuPencil />}
                onClick={() => onEdit?.(info.row.index)}
              />
              <IconButton
                aria-label={t('rjsf.MqttTransformationField.actions.delete.aria-label')}
                icon={<LuTrash />}
                onClick={() => onDelete?.(info.row.index)}
              />
            </ButtonGroup>
          )
        },
        footer: () => {
          return (
            <ButtonGroup isAttached size="xs">
              <IconButton
                aria-label={t('rjsf.MqttTransformationField.actions.add.aria-label')}
                icon={<LuPlus />}
                onClick={() => onAdd?.()}
              />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [onAdd, onDelete, onEdit, t])

  return (
    <Card>
      <PaginatedTable<FieldMappingsModel>
        aria-label={t('rjsf.MqttTransformationField.tabs.list')}
        data={items}
        columns={columns}
        enablePagination={false}
      />
    </Card>
  )
}

export default ListMappings
