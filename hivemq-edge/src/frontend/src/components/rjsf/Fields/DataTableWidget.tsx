import { FC, useEffect, useMemo, useState } from 'react'
import { ColumnDef, flexRender, getCoreRowModel, RowData, Table, useReactTable } from '@tanstack/react-table'
import { Box, Input, chakra as Chakra } from '@chakra-ui/react'
import { LuPlus, LuTrash2 } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'

import { DataGridProps, FormDataItem, CustomPropertyValue } from '@/components/rjsf/Fields/types.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'

declare module '@tanstack/react-table' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface TableMeta<TData extends RowData> {
    updateData?: (rowIndex: number, columnId: string, value: CustomPropertyValue) => void
  }
}

interface CellEditProps {
  initialValue: CustomPropertyValue
  table: Table<FormDataItem>
  index: number
  columnId: string
  isRequired?: boolean
}

const DefaultCell: FC<CellEditProps> = ({ table, initialValue, columnId, index, isRequired }) => {
  const { t } = useTranslation('components')
  const [value, setValue] = useState(initialValue)

  const onBlur = () => {
    table.options.meta?.updateData?.(index, columnId, value)
  }

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
      // size="sm"
      aria-label={t('rjsf.CompactArrayField.table.cell', { column: columnId, row: index })}
    />
  )
}

const DataTableWidget: FC<DataGridProps> = ({
  data,
  columnTypes,
  isDisabled,
  required,
  maxItems,
  onHandleDeleteItem,
  onHandleAddItem,
  onUpdateData,
}) => {
  const { t } = useTranslation('components')
  const columns = useMemo<ColumnDef<FormDataItem>[]>(() => {
    const columns = columnTypes.map<ColumnDef<FormDataItem>>(([key, value]) => ({
      header: value.title,
      accessorKey: key,
    }))
    const isFull = maxItems !== undefined && data.length > maxItems - 1

    columns.push({
      header: t('rjsf.CompactArrayField.table.actions'),
      id: 'actions',
      cell: (props) => {
        return (
          <IconButton
            icon={<LuTrash2 />}
            aria-label={t('rjsf.CompactArrayField.action.delete')}
            size="sm"
            isDisabled={isDisabled}
            onClick={() => onHandleDeleteItem?.(props.row.index)}
          />
        )
      },
      footer: () => {
        return (
          <IconButton
            icon={<LuPlus />}
            aria-label={t('rjsf.CompactArrayField.action.add')}
            size="sm"
            isDisabled={isDisabled || isFull}
            onClick={onHandleAddItem}
          />
        )
      },
    })
    return columns
  }, [columnTypes, isDisabled, onHandleAddItem, onHandleDeleteItem, t])

  const table = useReactTable({
    data,
    columns,
    defaultColumn: {
      cell: ({ getValue, row: { index }, column: { id }, table }) => {
        return (
          <DefaultCell
            initialValue={getValue<CustomPropertyValue>()}
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
      updateData: onUpdateData,
    },
  })

  return (
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
  )
}

export default DataTableWidget
