import { useState, CSSProperties } from 'react'
import {
  ColumnDef,
  ColumnFiltersState,
  flexRender,
  getCoreRowModel,
  getFacetedMinMaxValues,
  getFacetedRowModel,
  getFacetedUniqueValues,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  Row,
  useReactTable,
} from '@tanstack/react-table'
import { Table as TableUI, Thead, Tbody, Tr, Th, Td, TableContainer, Text, Alert } from '@chakra-ui/react'

import PaginationBar from './components/PaginationBar.tsx'
import { useTranslation } from 'react-i18next'

interface PaginatedTableProps<T> {
  data: Array<T>
  columns: ColumnDef<T>[]
  pageSizes?: number[]
  noDataText?: string
  /**
   * Define row styles
   */
  getRowStyles?: (row: Row<T>) => CSSProperties
}

const DEFAULT_PAGE_SIZES = [5, 10, 20, 30, 40, 50]

const PaginatedTable = <T,>({
  data,
  columns,
  pageSizes = DEFAULT_PAGE_SIZES,
  noDataText,
  getRowStyles,
}: PaginatedTableProps<T>) => {
  const { t } = useTranslation()
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [globalFilter, setGlobalFilter] = useState('')

  const table = useReactTable({
    data: data,
    columns,
    state: {
      columnFilters,
      globalFilter,
    },
    onColumnFiltersChange: setColumnFilters,
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues(),
    getFacetedMinMaxValues: getFacetedMinMaxValues(),
  })

  return (
    <>
      <TableContainer overflowY={'auto'} overflowX={'auto'} whiteSpace={'normal'}>
        <TableUI variant="simple" size={'sm'}>
          <Thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <Tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  return (
                    <Th key={header.id} colSpan={header.colSpan}>
                      {header.isPlaceholder ? null : (
                        <Text
                          {...{
                            className: header.column.getCanSort() ? 'cursor-pointer select-none' : '',
                            onClick: header.column.getToggleSortingHandler(),
                          }}
                        >
                          {flexRender(header.column.columnDef.header, header.getContext())}
                          {{
                            asc: ' 🔼',
                            desc: ' 🔽',
                          }[header.column.getIsSorted() as string] ?? null}
                        </Text>
                      )}
                    </Th>
                  )
                })}
              </Tr>
            ))}
          </Thead>
          <Tbody>
            {table.getRowModel().rows.length === 0 && (
              <Tr>
                <Td colSpan={table.getAllColumns().length}>
                  <Alert status="info">
                    {noDataText === undefined ? t('components:pagination.noDataText') : noDataText}
                  </Alert>
                </Td>
              </Tr>
            )}
            {table.getRowModel().rows.length !== 0 &&
              table.getRowModel().rows.map((row) => {
                return (
                  <Tr key={row.id} style={{ ...getRowStyles?.(row) }}>
                    {row.getVisibleCells().map((cell) => {
                      return <Td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</Td>
                    })}
                  </Tr>
                )
              })}
          </Tbody>
        </TableUI>
      </TableContainer>
      <PaginationBar table={table} pageSizes={pageSizes} />
    </>
  )
}

export default PaginatedTable
