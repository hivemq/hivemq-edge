import type { FC } from 'react'
import { useMemo } from 'react'
import type { CellContext, ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'

import { Skeleton, Text } from '@chakra-ui/react'

import type { PolicySchema } from '@/api/__generated__'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { useDeleteSchema } from '@datahub/api/hooks/DataHubSchemasService/useDeleteSchema.tsx'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import type { DataHubTableProps } from '@datahub/components/pages/DataHubListings.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { downloadJSON } from '@datahub/utils/download.utils.ts'

const SchemaTable: FC<DataHubTableProps> = ({ onDeleteItem }) => {
  const { t } = useTranslation('datahub')
  const { isLoading, data, isError } = useGetAllSchemas()
  const deleteSchema = useDeleteSchema()

  const safeData = useMemo<PolicySchema[]>(() => {
    if (isLoading) return [mockSchemaTempHumidity]

    return [...(data?.items || [])]
  }, [isLoading, data?.items])

  const columns = useMemo<ColumnDef<PolicySchema>[]>(() => {
    const onHandleDownload = (info: CellContext<PolicySchema, unknown>) => () => {
      downloadJSON<PolicySchema>(info.row.original.id, info.row.original)
    }

    return [
      {
        accessorKey: 'id',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <Text>{info.getValue<string>()}</Text>
          </Skeleton>
        ),
        header: t('Listings.schema.header.id'),
      },
      {
        accessorKey: 'type',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>{t('resource.schema.type', { context: info.getValue<string>() })}</Text>
            </Skeleton>
          )
        },
        header: t('Listings.schema.header.type'),
      },
      {
        accessorKey: 'version',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>{info.getValue<string>()}</Text>
            </Skeleton>
          )
        },
        header: t('Listings.schema.header.version'),
      },
      {
        accessorKey: 'createdAt',
        sortType: 'datetime',
        accessorFn: (row) => (row.createdAt ? DateTime.fromISO(row.createdAt).toMillis() : undefined),
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <DateTimeRenderer date={DateTime.fromMillis(info.getValue() as number)} isApprox />
          </Skeleton>
        ),
        header: t('Listings.schema.header.created'),
      },
      {
        id: 'actions',
        header: t('Listings.schema.header.actions'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DataHubListAction
                onDelete={() => onDeleteItem?.(deleteSchema.mutateAsync, DataHubNodeType.SCHEMA, info.row.original.id)}
                onDownload={onHandleDownload(info)}
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [deleteSchema.mutateAsync, isLoading, onDeleteItem, t])

  return (
    <PaginatedTable<PolicySchema>
      aria-label={t('Listings.schema.label')}
      data={safeData}
      columns={columns}
      isError={isError}
    />
  )
}

export default SchemaTable
