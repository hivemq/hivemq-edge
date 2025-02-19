import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, HStack } from '@chakra-ui/react'
import type { RJSFSchema, FormContextType, WidgetProps } from '@rjsf/utils'
import { LuTrash } from 'react-icons/lu'

import type { DataCombining } from '@/api/__generated__'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import IconButton from '@/components/Chakra/IconButton'
import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag'

export const DataCombiningMappingWidget: FC<WidgetProps<DataCombining[], RJSFSchema, FormContextType>> = (props) => {
  const { t } = useTranslation()

  const displayColumns = useMemo<ColumnDef<DataCombining>[]>(() => {
    return [
      {
        accessorKey: 'id',
      },
      {
        accessorKey: 'destination',
        cell: (info) => {
          return <Topic tagTitle={info.row.original.destination} />
        },
      },
      {
        accessorKey: 'sources',
        cell: (info) => {
          return (
            <HStack flexWrap={'wrap'}>
              {info.row.original.sources?.tags?.map((tag) => <PLCTag tagTitle={tag} key={tag} />)}
              {info.row.original.sources?.topicFilters?.map((tag) => <TopicFilter tagTitle={tag} key={tag} />)}
            </HStack>
          )
        },
      },

      {
        id: 'actions',
        header: t('combiner.schema.mappings.table.action'),
        sortingFn: undefined,
        cell: () => {
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton
                aria-label={t('combiner.schema.mappings.table.delete')}
                icon={<LuTrash />}
                isDisabled={true}
              />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [t])

  return (
    <PaginatedTable<DataCombining>
      aria-label={t('combiner.schema.sources.description')}
      data={props.value}
      columns={displayColumns}
      enablePaginationGoTo={false}
      enablePaginationSizes={false}
      enableColumnFilters={false}
    />
  )
}
