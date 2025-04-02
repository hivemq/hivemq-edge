import { type FC, useMemo } from 'react'
import { ButtonGroup } from '@chakra-ui/react'
import { type FieldProps, type RJSFSchema, getUiOptions, getTemplate } from '@rjsf/utils'
import type { ColumnDef } from '@tanstack/react-table'
import { useTranslation } from 'react-i18next'
import { LuPencil, LuTrash } from 'react-icons/lu'

import type { DomainTag } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import { PLCTag } from '@/components/MQTT/EntityTag'

export const TagTableField: FC<FieldProps<DomainTag[], RJSFSchema>> = (props) => {
  const { t } = useTranslation()
  const { schema, registry, uiSchema } = props

  const fieldOptions = getUiOptions(uiSchema)

  const TitleFieldTemplate = getTemplate<'TitleFieldTemplate'>(
    'TitleFieldTemplate',
    registry,
    // @ts-ignore weird TS error on fieldOptions
    fieldOptions
  )
  const DescriptionFieldTemplate = getTemplate<'DescriptionFieldTemplate'>(
    'DescriptionFieldTemplate',
    registry,
    // @ts-ignore weird TS error on fieldOptions
    fieldOptions
  )

  const displayColumns = useMemo<ColumnDef<DomainTag>[]>(() => {
    return [
      {
        accessorKey: 'name',
        header: t('device.drawer.table.column.name'),
        cell: (info) => {
          return <PLCTag tagTitle={info.row.original.name} />
        },
      },
      {
        accessorKey: 'description',
        header: t('device.drawer.table.column.description'),
      },
      {
        id: 'actions',
        header: t('combiner.schema.sources.table.action'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton
                aria-label={t('device.drawer.table.actions.edit')}
                icon={<LuPencil />}
                onClick={() => console.log(info.row.index)}
              />
              <IconButton aria-label={t('device.drawer.table.actions.delete')} icon={<LuTrash />} isDisabled={true} />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [t])
  if (schema.type !== 'array') throw new Error('[RJSF] Cannot apply the template to the schema')
  return (
    <>
      <TitleFieldTemplate
        id="root_items__title"
        title={props.uiSchema?.title || props.schema.title}
        schema={props.schema}
        registry={registry}
      />
      <DescriptionFieldTemplate
        id="root_items__description"
        description={props.uiSchema?.description || props.schema.description}
        schema={props.schema}
        registry={registry}
      />
      <PaginatedTable<DomainTag>
        aria-label={t('combiner.schema.sources.description')}
        data={props.formData || []}
        columns={displayColumns}
        enablePaginationGoTo={true}
        enablePaginationSizes={true}
        enableColumnFilters={false}
      />
    </>
  )
}
