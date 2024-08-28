import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, Card, Text } from '@chakra-ui/react'
import { LuPencil, LuPlus, LuTrash } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import Topic from '@/components/MQTT/Topic.tsx'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'

interface ListSubscriptionsProps {
  items: OutwardSubscription[]
  onEdit?: (index: number) => void
  onDelete?: (index: number) => void
  onAdd?: () => void
  isDisabled: boolean
}

const ListSubscriptions: FC<ListSubscriptionsProps> = ({ items, onEdit, onDelete, onAdd, isDisabled }) => {
  const { t } = useTranslation('components')

  const columns = useMemo<ColumnDef<OutwardSubscription>[]>(() => {
    return [
      {
        accessorKey: 'mqtt-topic',
        enableSorting: false,
        cell: (info) => {
          const values = info.getValue<string[]>()
          if (!values.length) return <Text>{t('rjsf.MqttTransformationField.unset')}</Text>
          return values.map((e) => <Topic key={e} topic={e} />)
        },
        header: t('rjsf.MqttTransformationField.listing.sources'),
      },
      {
        accessorKey: 'node',
        cell: (info) => {
          const val = info.getValue<string>()
          return val ? <Text>{val}</Text> : <Text>{t('rjsf.MqttTransformationField.unset')}</Text>
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
            <ButtonGroup isAttached size="xs" isDisabled={isDisabled}>
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
            <ButtonGroup isAttached size="xs" isDisabled={isDisabled}>
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
  }, [isDisabled, onAdd, onDelete, onEdit, t])

  return (
    <Card>
      <PaginatedTable<OutwardSubscription>
        aria-label="list of mapping"
        data={items}
        columns={columns}
        enablePagination={false}
      />
    </Card>
  )
}

export default ListSubscriptions
