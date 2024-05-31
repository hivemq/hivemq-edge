import { FC, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import validator from '@rjsf/validator-ajv8'
import { ColumnDef } from '@tanstack/react-table'
import { Button, Checkbox, FormControl, FormLabel, Switch, Tag, Text, Tooltip, useBoolean } from '@chakra-ui/react'
import { LuCheckSquare } from 'react-icons/lu'

import { StepRendererProps, ValidationColumns } from '@/components/rjsf/BatchSubscription/types.ts'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'

interface ValidationColumns extends ErrorObject {
  [x: string]: unknown
  row: number
  isError?: boolean
}

const SubscriptionsValidationStep: FC<StepRendererProps> = ({ store }) => {
  const { t } = useTranslation('components')
  const [flagError, setFlagError] = useBoolean()

  const columns = useMemo<ColumnDef<ValidationColumns>[]>(() => {
    const { mapping } = store
    const required = mapping ?? []

    const columns: ColumnDef<ValidationColumns>[] = [
      {
        id: 'group',
        footer: ({ table }) => (
          <Button
            leftIcon={<LuCheckSquare />}
            isDisabled={!table.getIsSomeRowsSelected() && !table.getIsAllRowsSelected()}
          >
            {t('rjsf.batchUpload.dataValidation.action.delete', { count: table.getSelectedRowModel().rows.length })}
          </Button>
        ),
        columns: [
          {
            id: 'select',
            enableSorting: false,
            cell: ({ row }) => {
              const { isError } = row.original
              return (
                <Checkbox
                  isChecked={row.getIsSelected()}
                  onChange={row.getToggleSelectedHandler()}
                  colorScheme={isError ? 'red' : 'blue'}
                />
              )
            },
            header: ({ table }) => {
              return (
                <Checkbox
                  isChecked={table.getIsAllRowsSelected()}
                  isIndeterminate={table.getIsSomeRowsSelected()}
                  onChange={table.getToggleAllRowsSelectedHandler()}
                />
              )
            },
          },
          {
            accessorKey: 'row',
            enableSorting: false,
          },
        ],
      },
      {
        id: 'data',
        columns: required.map<ColumnDef<ValidationColumns>>((columnId) => ({
          header: columnId.subscription,
          accessorKey: columnId.subscription,
          id: columnId.subscription,
          enableSorting: false,
          cell: (info) => {
            const { errors } = info.row.original
            const cellError = errors?.find((error) => {
              // TODO[NVL] need to check for other types of error
              const isTypeError = () => error.instancePath.includes(info.cell.column.id)
              const isRequiredError = () =>
                error.keyword === 'required' && error.params.missingProperty === info.cell.column.id
              return isTypeError() || isRequiredError()
            })

            if (cellError) {
              return (
                <Tooltip label={cellError.message}>
                  <Tag colorScheme="red" minW={75}>
                    {info.getValue<string>()}
                  </Tag>
                </Tooltip>
              )
            }

            return <Text>{info.getValue<string>()}</Text>
          },
        })),
        footer: () => (
          <FormControl display="flex" alignItems="center" justifyContent="flex-end">
            <FormLabel htmlFor="email-alerts" mb="0">
              Show errors only
            </FormLabel>
            <Switch id="email-alerts" isChecked={flagError.valueOf()} onChange={setFlagError.toggle} />
          </FormControl>
        ),
      },
    ]

    return columns
  }, [flagError, setFlagError.toggle, store, t])

  const data = useMemo<ValidationColumns[]>(() => {
    if (!store.mapping || !store.worksheet || !store.schema.items) return []

    const validate = validator.ajv.compile(store.schema.items)

    const rows = store.worksheet.map((row, index) => {
      const mappedData = store.mapping?.reduce<ValidationColumns>(
        (acc, elt) => {
          acc[elt.subscription] = row[elt.column]
          return acc
        },
        { row: index }
      )

      const ggg = validate(mappedData)
      return { ...mappedData, isError: !ggg, errors: validate.errors } as ValidationColumns
    })

    return flagError ? rows.filter((e) => e.isError) : rows
  }, [flagError, store.mapping, store.schema.items, store.worksheet])

  return (
    <PaginatedTable<ValidationColumns>
      noDataText={t('rjsf.batchUpload.dataValidation.table.noDataText')}
      aria-label="sss"
      data={data}
      columns={columns}
      enablePagination={true}
      pageSizes={[10]}
      enablePaginationSizes={false}
      enablePaginationGoTo={false}
    />
  )
}

export default SubscriptionsValidationStep
