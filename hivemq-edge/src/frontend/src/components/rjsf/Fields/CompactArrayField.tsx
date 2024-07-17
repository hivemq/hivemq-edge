import { FieldProps, getTemplate, getUiOptions } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

import { FC, useCallback, useEffect, useMemo, useState } from 'react'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { JSONSchema7 } from 'json-schema'
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  RowData,
  Table,
  useReactTable,
} from '@tanstack/react-table'
import { Box, chakra as Chakra, Input } from '@chakra-ui/react'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { LuPlus, LuTrash2 } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'

type FormDataValue = string
type FormDataItem = Record<string, FormDataValue>
type FormData = FormDataItem[]

declare module '@tanstack/react-table' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface TableMeta<TData extends RowData> {
    updateData: (rowIndex: number, columnId: string, value: FormDataValue) => void
  }
}

interface CellEditProps {
  initialValue: FormDataValue
  table: Table<FormDataItem>
  index: number
  columnId: string
  isRequired?: boolean
}

const DefaultCell: FC<CellEditProps> = ({ table, initialValue, columnId, index, isRequired }) => {
  const { t } = useTranslation('components')
  // We need to keep and update the state of the cell normally
  const [value, setValue] = useState(initialValue)

  // When the input is blurred, we'll call our table meta's updateData function
  const onBlur = () => {
    table.options.meta?.updateData(index, columnId, value)
  }

  // If the initialValue is changed external, sync it up with our state
  useEffect(() => {
    setValue(initialValue)
  }, [initialValue])

  return (
    <Input
      isRequired={isRequired}
      isInvalid={isRequired && !value}
      value={value as string}
      onChange={(e) => setValue(e.target.value)}
      onBlur={onBlur}
      size="sm"
      aria-label={t('rjsf.CompactArrayField.table.cell', { column: columnId, row: index })}
    />
  )
}

const CompactArrayField: FC<FieldProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const { idSchema, registry, formData, schema, disabled, readonly } = props
  const uiOptions = getUiOptions(props.uiSchema)
  const [rawData, setRawData] = useState<FormData>((formData || []) as FormData)

  const { items } = schema
  const { properties, maxItems, required } = items as JSONSchema7

  const columnTypes = useMemo(() => {
    if (!properties) return []
    return Object.entries(properties) as [string, JSONSchema7][]
  }, [properties])

  const getNewItem = useCallback(() => {
    return Object.fromEntries(columnTypes.map(([columnId]) => [columnId, ''])) as FormDataItem
  }, [columnTypes])

  const onHandleAdd = () => {
    const newData = getNewItem()
    setRawData((old) => [...old, newData])
  }

  const onHandleDelete = (index: number) => {
    setRawData((old) => {
      const newData = [...old]
      newData.splice(index, 1)
      return newData
    })
  }

  const isFull = maxItems !== undefined && rawData.length > maxItems - 1

  const columns = useMemo<ColumnDef<FormDataItem>[]>(() => {
    const columns = columnTypes.map<ColumnDef<FormDataItem>>((a) => ({
      header: a[1].title,
      accessorKey: a[0],
    }))
    columns.push({
      header: t('rjsf.CompactArrayField.table.actions'),
      id: 'actions',
      cell: (props) => {
        return (
          <IconButton
            icon={<LuTrash2 />}
            aria-label={t('rjsf.CompactArrayField.action.delete')}
            size="sm"
            isDisabled={disabled || readonly}
            onClick={() => onHandleDelete(props.row.index)}
          />
        )
      },
      footer: () => {
        return (
          <IconButton
            icon={<LuPlus />}
            aria-label={t('rjsf.CompactArrayField.action.add')}
            size="sm"
            isDisabled={disabled || readonly || isFull}
            onClick={onHandleAdd}
          />
        )
      },
    })
    return columns
  }, [columnTypes, disabled, isFull, onHandleAdd, readonly, t])

  const table = useReactTable({
    data: rawData,
    columns,
    defaultColumn: {
      cell: ({ getValue, row: { index }, column: { id }, table }) => {
        return (
          <DefaultCell
            initialValue={getValue<FormDataValue>()}
            table={table}
            index={index}
            columnId={id}
            isRequired={required?.includes(id)}
          />
        )
      },
    },
    getCoreRowModel: getCoreRowModel(),
    debugTable: true,
    meta: {
      updateData: (rowIndex, columnId, value) => {
        setRawData((old) => {
          if (old.length === 0) return [{ [columnId]: value }]

          const ret = [...old]
          ret[rowIndex] = { ...ret[rowIndex], [columnId]: value }

          return ret
        })
        // No access to "submit" event so need to store the data back in FormData
        props.onChange(rawData)
      },
    },
  })

  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate'>('ArrayFieldTitleTemplate', registry, uiOptions)
  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate'>(
    'ArrayFieldDescriptionTemplate',
    props.registry,
    uiOptions
  )

  // TODO Check for other conditions on the Schema and UISchema
  if (schema.type !== 'array') return <props.registry.fields.ArrayField {...props} />

  return (
    <>
      <ArrayFieldTitleTemplate
        idSchema={idSchema}
        title={uiOptions.title || props.schema.title}
        schema={props.schema}
        uiSchema={props.uiSchema}
        required={props.required}
        registry={registry}
      />
      <ArrayFieldDescriptionTemplate
        idSchema={idSchema}
        description={uiOptions.description || props.schema.description}
        schema={props.schema}
        uiSchema={props.uiSchema}
        registry={registry}
      />
      <Chakra.table w="100%">
        <Chakra.thead>
          {table.getHeaderGroups().map((headerGroup) => (
            <Chakra.tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                return (
                  <Chakra.th key={header.id} colSpan={header.colSpan} align="left">
                    {header.isPlaceholder ? null : (
                      <Box>{flexRender(header.column.columnDef.header, header.getContext())}</Box>
                    )}
                  </Chakra.th>
                )
              })}
            </Chakra.tr>
          ))}
        </Chakra.thead>
        <Chakra.tbody>
          {table.getRowModel().rows.map((row) => (
            <Chakra.tr key={row.id}>
              {row.getVisibleCells().map((cell) => {
                return <Chakra.td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</Chakra.td>
              })}
            </Chakra.tr>
          ))}
        </Chakra.tbody>
        <Chakra.tfoot>
          {table.getFooterGroups().map((footerGroup) => (
            <Chakra.tr key={footerGroup.id}>
              {footerGroup.headers.map((header) => {
                return (
                  <Chakra.th key={header.id} align="left">
                    {header.isPlaceholder ? null : flexRender(header.column.columnDef.footer, header.getContext())}
                  </Chakra.th>
                )
              })}
            </Chakra.tr>
          ))}
        </Chakra.tfoot>
      </Chakra.table>
    </>
  )
}

export default CompactArrayField
