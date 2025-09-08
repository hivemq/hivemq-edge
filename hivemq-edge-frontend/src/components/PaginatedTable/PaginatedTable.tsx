import type { CSSProperties } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { ColumnDef, ColumnFiltersState, ExpandedState, Row } from '@tanstack/react-table'
import {
  flexRender,
  getCoreRowModel,
  getExpandedRowModel,
  getFacetedMinMaxValues,
  getFacetedRowModel,
  getFacetedUniqueValues,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
} from '@tanstack/react-table'
import {
  Table,
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
  Tfoot,
} from '@chakra-ui/react'
import { BiSortDown, BiSortUp } from 'react-icons/bi'

import { getAriaSort } from '@/components/PaginatedTable/utils/table-utils.ts'
import SearchBar from '@/components/PaginatedTable/components/SearchBar.tsx'
import PaginationBar from '@/components/PaginatedTable/components/PaginationBar.tsx'
import { Filter } from '@/components/PaginatedTable/components/Filter.tsx'
import TableToolBar from '@/components/PaginatedTable/components/TableToolBar.tsx'

interface PaginatedTableProps<T> {
  data: Array<T>
  columns: ColumnDef<T>[]
  pageSizes?: number[]
  noDataText?: string
  enableColumnFilters?: boolean
  enablePagination?: boolean
  enablePaginationSizes?: boolean
  enablePaginationGoTo?: boolean
  enableGlobalFilter?: boolean
  isError?: boolean
  'aria-label': string
  /**
   * Define row styles
   */
  getRowStyles?: (row: Row<T>) => CSSProperties
  getSubRows?: (originalRow: T, index: number) => undefined | T[]
  customControls?: React.ReactNode
  initState?: {
    columnFilters?: ColumnFiltersState
    globalFilter?: string
  }
}

const DEFAULT_PAGE_SIZES = [5, 10, 20, 30, 40, 50]

const PaginatedTable = <T,>({
  data,
  columns,
  pageSizes = DEFAULT_PAGE_SIZES,
  noDataText,
  getRowStyles,
  enableColumnFilters = false,
  enableGlobalFilter = false,
  enablePagination = true,
  enablePaginationSizes = true,
  enablePaginationGoTo = true,
  isError = false,
  getSubRows = undefined,
  'aria-label': ariaLabel,
  customControls = undefined,
  initState,
}: PaginatedTableProps<T>) => {
  const { t } = useTranslation()
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>(initState?.columnFilters || [])
  const [globalFilter, setGlobalFilter] = useState(initState?.globalFilter || '')
  const [rowSelection, setRowSelection] = useState({})
  const [expanded, setExpanded] = useState<ExpandedState>({})

  const table = useReactTable({
    data: data,
    columns,
    initialState: { pagination: { pageSize: 5 } },
    state: {
      columnFilters,
      globalFilter,
      rowSelection,
      expanded,
    },
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    enableColumnFilters: enableColumnFilters,
    onColumnFiltersChange: setColumnFilters,
    onGlobalFilterChange: setGlobalFilter,
    getSubRows,
    onExpandedChange: setExpanded,
    getExpandedRowModel: getExpandedRowModel(),
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues(),
    getFacetedMinMaxValues: getFacetedMinMaxValues(),
  })

  const hasFooters =
    table
      .getFooterGroups()
      .map((group) => group.headers.map((header) => header.column.columnDef.footer))
      .flat()
      .filter(Boolean).length > 0

  return (
    <>
      <TableContainer overflowY="auto" overflowX="auto" whiteSpace="normal" data-testid="table-container">
        <TableToolBar
          leftControls={
            (enableGlobalFilter || enableColumnFilters) && (
              <SearchBar
                setGlobalFilter={(value) => table.setGlobalFilter(value)}
                resetColumnFilters={table.resetColumnFilters}
                globalFilter={table.getState().globalFilter}
                columnFilters={table.getState().columnFilters}
                enableGlobalFilter={enableGlobalFilter}
                enableColumnFilters={enableColumnFilters}
              />
            )
          }
          rightControls={customControls}
        />

        <Table variant="simple" size="sm" aria-label={ariaLabel}>
          <Thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <Tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <Th
                    key={header.id}
                    colSpan={header.colSpan}
                    verticalAlign="top"
                    aria-sort={getAriaSort(header.column.getCanSort(), header.column.getIsSorted())}
                  >
                    <VStack alignItems="flex-start">
                      {header.isPlaceholder && null}
                      {!header.isPlaceholder && header.column.getCanSort() && (
                        <Button
                          px={1}
                          onClick={header.column.getToggleSortingHandler()}
                          size="sm"
                          variant="ghost"
                          textTransform="inherit"
                          fontWeight="inherit"
                          fontSize="inherit"
                          height="24px"
                          userSelect="none"
                          rightIcon={
                            {
                              asc: <Icon as={BiSortUp} fontSize="24px" />,
                              desc: <Icon as={BiSortDown} fontSize="24px" />,
                            }[header.column.getIsSorted() as string] ?? undefined
                          }
                        >
                          {flexRender(header.column.columnDef.header, header.getContext())}
                        </Button>
                      )}
                      {!header.isPlaceholder && !header.column.getCanSort() && (
                        <Text userSelect="none" pt={1}>
                          {flexRender(header.column.columnDef.header, header.getContext())}
                        </Text>
                      )}
                      {header.column.getCanFilter() && (
                        <Filter<T>
                          {...header.column}
                          firstValue={table.getPreFilteredRowModel().flatRows[0]?.getValue(header.column.id)}
                        />
                      )}
                    </VStack>
                  </Th>
                ))}
              </Tr>
            ))}
          </Thead>
          <Tbody>
            {table.getRowModel().rows.length === 0 && (
              <Tr>
                <Td colSpan={table.getAllFlatColumns().length}>
                  {isError ? (
                    <Alert status="error">{t('components:pagination.noDataLoadError')}</Alert>
                  ) : (
                    <Alert status="info">
                      {table.getCoreRowModel().rows.length === 0
                        ? noDataText || t('components:pagination.noDataText')
                        : t('components:pagination.noDataFiltered')}
                    </Alert>
                  )}
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
          {hasFooters && (
            <Tfoot>
              {table.getFooterGroups().map((footerGroup) => (
                <Tr key={footerGroup.id}>
                  {footerGroup.headers.map((header) => (
                    <Td key={header.id} colSpan={header.colSpan}>
                      {header.isPlaceholder ? null : flexRender(header.column.columnDef.footer, header.getContext())}
                    </Td>
                  ))}
                </Tr>
              ))}
            </Tfoot>
          )}
        </Table>
      </TableContainer>
      {enablePagination && (
        <PaginationBar table={table} pageSizes={pageSizes} options={{ enablePaginationSizes, enablePaginationGoTo }} />
      )}
    </>
  )
}

export default PaginatedTable
