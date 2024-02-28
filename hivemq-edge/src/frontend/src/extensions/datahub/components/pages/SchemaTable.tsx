import { FC, useMemo } from 'react'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import { Skeleton, Text } from '@chakra-ui/react'

import type { Schema } from '@/api/__generated__'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { useDeleteSchema } from '@datahub/api/hooks/DataHubSchemasService/useDeleteSchema.tsx'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'

const SchemaTable: FC = () => {
  const { t } = useTranslation('datahub')
  const { isLoading, data, isError } = useGetAllSchemas()
  const deleteSchema = useDeleteSchema()
  const navigate = useNavigate()

  const safeData = useMemo<Schema[]>(() => {
    if (isLoading) return [mockSchemaTempHumidity]

    return [...(data?.items || [])]
  }, [isLoading, data?.items])

  const columns = useMemo<ColumnDef<Schema>[]>(() => {
    return [
      {
        accessorKey: 'id',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <Text>{info.getValue<string>()}</Text>
          </Skeleton>
        ),
        header: t('Listings.schema.header.id') as string,
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
        header: t('Listings.schema.header.type') as string,
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
        header: t('Listings.schema.header.version') as string,
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
        header: t('Listings.schema.header.created') as string,
      },
      {
        id: 'actions',
        header: t('Listings.schema.header.actions') as string,
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DataHubListAction
                isEditDisabled={true}
                onDelete={() =>
                  deleteSchema
                    .mutateAsync(info.row.original.id)
                    .then((e) => console.log('XXXXXX', e))
                    .catch((e) => console.log('XXXXX', e.toString(), info))
                }
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [isLoading, navigate, t])

  return (
    <PaginatedTable<Schema>
      aria-label={t('Listings.schema.label')}
      data={safeData}
      columns={columns}
      isError={isError}
    />
  )
}

export default SchemaTable
