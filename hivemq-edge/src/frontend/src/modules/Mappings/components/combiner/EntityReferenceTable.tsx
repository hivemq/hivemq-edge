import { useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import type { RJSFSchema } from '@rjsf/utils/src/types'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup } from '@chakra-ui/react'
import { LuTrash } from 'react-icons/lu'

import type { EntityReference } from '@/api/__generated__'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import IconButton from '@/components/Chakra/IconButton'
import { EntityRenderer } from './EntityRenderer'

const EntityReferenceTable = (props: WidgetProps<WidgetProps<Array<EntityReference>, RJSFSchema>>) => {
  const { t } = useTranslation()
  const { schema } = props

  const displayColumns = useMemo<ColumnDef<EntityReference>[]>(() => {
    return [
      {
        accessorKey: 'type',
        header: t('combiner.schema.sources.table.source'),
        cell: (info) => {
          return <EntityRenderer reference={info.row.original} />
        },
      },
      {
        id: 'actions',
        header: t('combiner.schema.sources.table.action'),
        sortingFn: undefined,
        cell: () => {
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton aria-label={t('combiner.schema.sources.table.delete')} icon={<LuTrash />} isDisabled={true} />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [t])

  if (schema.type !== 'array') throw new Error('[RJSF] Cannot apply the template to the schema')
  return (
    <PaginatedTable<EntityReference>
      aria-label={t('combiner.schema.sources.description')}
      data={props.value}
      columns={displayColumns}
      enablePaginationGoTo={false}
      enablePaginationSizes={false}
      enableColumnFilters={false}
    />
  )
}

export default EntityReferenceTable
