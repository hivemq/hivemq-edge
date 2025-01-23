import type { FC } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import validator from '@rjsf/validator-ajv8'
import type { ColumnDef } from '@tanstack/react-table'
import {
  Button,
  Checkbox,
  FormControl,
  FormLabel,
  Switch,
  Tag,
  Text,
  Tooltip,
  useBoolean,
  VisuallyHidden,
} from '@chakra-ui/react'
import { LuCheckSquare } from 'react-icons/lu'

import type { StepRendererProps, ValidationColumns } from '@/components/rjsf/BatchModeMappings/types.ts'
import { ErrorObject } from '@/components/rjsf/BatchModeMappings/types.ts'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'

const MappingsValidationStep: FC<StepRendererProps> = ({ store, onContinue }) => {
  const { t } = useTranslation('components')
  const [flagError, setFlagError] = useBoolean()

  const columns = useMemo<ColumnDef<ValidationColumns>[]>(() => {
    const { mapping } = store
    const required = mapping ?? []

    const columns: ColumnDef<ValidationColumns>[] = [
      {
        id: 'group',
        header: () => <VisuallyHidden>{t('rjsf.batchUpload.dataValidation.columns.admin')}</VisuallyHidden>,
        footer: ({ table }) => (
          <Button
            leftIcon={<LuCheckSquare />}
            isDisabled={!table.getIsSomeRowsSelected() && !table.getIsAllRowsSelected()}
            onClick={() => {
              const selectedRows = table.getSelectedRowModel().rows
              const selectedIDs = selectedRows.map((row) => row.original.row)
              setSubscriptions((old) => old.filter((subscription) => !selectedIDs.includes(subscription.row)))
              table.resetRowSelection()
            }}
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
                  aria-label={t('rjsf.batchUpload.dataValidation.columns.selectRow')}
                  isChecked={row.getIsSelected()}
                  onChange={row.getToggleSelectedHandler()}
                  colorScheme={isError ? 'red' : 'blue'}
                />
              )
            },
            header: ({ table }) => {
              return (
                <Checkbox
                  aria-label={t('rjsf.batchUpload.dataValidation.columns.selectHeader')}
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
        header: () => <VisuallyHidden>{t('rjsf.batchUpload.dataValidation.columns.subscription')}</VisuallyHidden>,
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
                error.keyword === ErrorObject.keyword.REQUIRED && error.params.missingProperty === info.cell.column.id
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
            <FormLabel htmlFor="show-errors" mb="0">
              {t('rjsf.batchUpload.dataValidation.action.errorOnly')}
            </FormLabel>
            <Switch id="show-errors" isChecked={flagError.valueOf()} onChange={setFlagError.toggle} />
          </FormControl>
        ),
      },
    ]

    return columns
  }, [flagError, setFlagError.toggle, store, t])

  const data = useMemo<ValidationColumns[]>(() => {
    if (!store.mapping || !store.worksheet || !store.schema.items) return []

    const validate = validator.ajv.compile(store.schema.items)

    return store.worksheet.map((row, index) => {
      const mappedData = store.mapping?.reduce<ValidationColumns>(
        (acc, elt) => {
          acc[elt.subscription] = row[elt.column]
          return acc
        },
        { row: index }
      )

      const isValid = validate(mappedData)
      return { ...mappedData, isError: !isValid, errors: validate.errors } as ValidationColumns
    })
  }, [store.mapping, store.schema.items, store.worksheet])
  const [subscriptions, setSubscriptions] = useState<ValidationColumns[]>(data)
  const isSubscriptionsValid = useMemo(() => {
    return subscriptions.length && subscriptions.every((subscription) => !subscription.isError)
  }, [subscriptions])

  useEffect(() => {
    onContinue({ subscriptions: isSubscriptionsValid ? subscriptions : undefined })
  }, [isSubscriptionsValid, onContinue, subscriptions])

  return (
    <PaginatedTable<ValidationColumns>
      noDataText={t('rjsf.batchUpload.dataValidation.table.noDataText')}
      aria-label={t('rjsf.batchUpload.dataValidation.table.ariaLabel')}
      data={flagError ? subscriptions.filter((subscription) => subscription.isError) : subscriptions}
      columns={columns}
      enablePagination={true}
      pageSizes={[10]}
      enablePaginationSizes={false}
      enablePaginationGoTo={false}
    />
  )
}

export default MappingsValidationStep
