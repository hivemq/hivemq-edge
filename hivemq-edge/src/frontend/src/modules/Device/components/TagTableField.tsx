import { type FC, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonGroup, Text, useColorModeValue, useToken } from '@chakra-ui/react'
import { type FieldProps, type RJSFSchema, getUiOptions, getTemplate } from '@rjsf/utils'
import type { ColumnDef, Row } from '@tanstack/react-table'
import { LuPencil, LuPlus, LuTrash, LuView } from 'react-icons/lu'

import type { DomainTag } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import { PLCTag } from '@/components/MQTT/EntityTag'
import type { DeviceTagListContext } from '../types'
import TagEditorDrawer from './TagEditorDrawer'
import TagSchemaDrawer from './TagSchemaDrawer'

export const TagTableField: FC<FieldProps<DomainTag[], RJSFSchema, DeviceTagListContext>> = (props) => {
  const { t } = useTranslation()
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)
  const itemErrorColor = useToken('colors', useColorModeValue('red.100', 'rgba(254, 178, 178, 0.16)'))

  const { schema, registry, uiSchema } = props
  const { adapterId } = props.formContext || {}

  const fieldOptions = getUiOptions(uiSchema)

  const TitleFieldTemplate = getTemplate<'TitleFieldTemplate', DomainTag[], RJSFSchema, DeviceTagListContext>(
    'TitleFieldTemplate',
    registry,
    fieldOptions
  )
  const DescriptionFieldTemplate = getTemplate<
    'DescriptionFieldTemplate',
    DomainTag[],
    RJSFSchema,
    DeviceTagListContext
  >('DescriptionFieldTemplate', registry, fieldOptions)

  const isRowError = (index: number) => Boolean(props.errorSchema?.[index])

  const displayColumns = useMemo<ColumnDef<DomainTag>[]>(() => {
    const isSchemaError = (index: number) => Boolean(props.errorSchema?.[index]?.definition)

    const handleAdd = () => {
      const newTag: DomainTag = {
        definition: {},
        name: '',
      }
      props.onChange([...(props.formData || []), newTag])
    }

    const handleDelete = (index: number) => {
      console.log(index)
      const newValues = [...(props.formData || [])]
      newValues.splice(index, 1)
      props.onChange(newValues)
    }

    return [
      {
        accessorKey: 'name',
        header: t('device.drawer.table.column.name'),
        cell: (info) => {
          if (info.row.original.name) return <PLCTag tagTitle={info.row.original.name} />
          return <Text>{t('device.drawer.table.unset')}</Text>
        },
      },
      {
        accessorKey: 'description',
        header: t('device.drawer.table.column.description'),
      },
      {
        id: 'actions',
        header: t('device.drawer.table.column.actions'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup role="toolbar">
              <ButtonGroup isAttached size="sm">
                <IconButton
                  data-testid={'tag-list-edit'}
                  aria-label={t('device.drawer.table.actions.edit')}
                  icon={<LuPencil />}
                  onClick={() => setSelectedItem(info.row.index)}
                  // colorScheme={isRowError(info.row.index) ? 'red' : 'black'}
                  // variant={isRowError(info.row.index) ? 'solid' : 'outline'}
                />
                <IconButton
                  data-testid={'tag-list-delete'}
                  aria-label={t('device.drawer.table.actions.delete')}
                  icon={<LuTrash />}
                  onClick={() => handleDelete(info.row.index)}
                />
              </ButtonGroup>
              <TagSchemaDrawer
                tag={info.row.original}
                adapterId={adapterId as string}
                trigger={({ onOpen: onOpenArrayDrawer }) => (
                  <ButtonGroup size="sm">
                    <IconButton
                      data-testid={'tag-list-schema'}
                      aria-label={t('device.drawer.table.actions.schema')}
                      icon={<LuView />}
                      onClick={onOpenArrayDrawer}
                      isDisabled={isSchemaError(info.row.index)}
                    />
                  </ButtonGroup>
                )}
              />
            </ButtonGroup>
          )
        },
        footer: () => {
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton
                data-testid={'tag-list-add'}
                aria-label={t('device.drawer.table.actions.add')}
                icon={<LuPlus />}
                onClick={handleAdd}
              />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [adapterId, props, t])
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
        aria-label={props.uiSchema?.title || props.schema.title}
        data={props.formData || []}
        columns={displayColumns}
        enablePaginationGoTo={true}
        enablePaginationSizes={true}
        enableColumnFilters={false}
        noDataText={t('device.errors.noTagCreated')}
        getRowStyles={(row: Row<DomainTag>) => {
          return isRowError(row.index) ? { backgroundColor: itemErrorColor, padding: '20px' } : {}
        }}
      />
      {selectedItem != null && props.formData?.[selectedItem] && (
        <TagEditorDrawer
          onClose={() => setSelectedItem(undefined)}
          onSubmit={() => console.log('XX')}
          schema={props.schema.items as RJSFSchema}
          uiSchema={props.uiSchema?.items}
          formData={props.formData?.[selectedItem]}
        />
      )}
    </>
  )
}
