import { useState, CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
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
import {
  Table as TableUI,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableContainer,
  Text,
  Alert,
  VStack,
  Button,
  Icon,
} from '@chakra-ui/react'

import PaginationBar from './components/PaginationBar.tsx'
import { Filter } from './components/Filter.tsx'
import { BiSortDown, BiSortUp } from 'react-icons/bi'

interface PaginatedTableProps<T> {
  data: Array<T>
  columns: ColumnDef<T>[]
  pageSizes?: number[]
  noDataText?: string
  enableColumnFilters?: boolean
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
  enableColumnFilters = false,
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
    enableColumnFilters: enableColumnFilters,
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
                {headerGroup.headers.map((header) => (
                  <Th key={header.id} colSpan={header.colSpan} verticalAlign={'top'}>
                    <VStack>
                      {header.isPlaceholder && null}
                      {!header.isPlaceholder && header.column.getCanSort() && (
                        <Button
                          onClick={header.column.getToggleSortingHandler()}
                          size={'sm'}
                          variant="ghost"
                          textTransform={'inherit'}
                          fontWeight={'inherit'}
                          fontSize={'inherit'}
                          height={'24px'}
                          userSelect={'none'}
                          rightIcon={
                            {
                              asc: <Icon as={BiSortUp} fontSize={'24px'} />,
                              desc: <Icon as={BiSortDown} fontSize={'24px'} />,
                            }[header.column.getIsSorted() as string] ?? undefined
                          }
                        >
                          {flexRender(header.column.columnDef.header, header.getContext())}
                        </Button>
                      )}
                      {!header.isPlaceholder && !header.column.getCanSort() && (
                        <Text userSelect={'none'} pt={1}>
                          {flexRender(header.column.columnDef.header, header.getContext())}
                        </Text>
                      )}
                      {header.column.getCanFilter() && <Filter<T> column={header.column} table={table} />}
                    </VStack>
                  </Th>
                ))}
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
