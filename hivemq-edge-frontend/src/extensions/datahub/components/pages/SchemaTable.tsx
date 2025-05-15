import type { FC } from 'react'
import { useMemo } from 'react'
import type { CellContext, ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { Skeleton, Text } from '@chakra-ui/react'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { downloadJSON } from '@/utils/download.utils.ts'

import { useDeleteSchema } from '@datahub/api/hooks/DataHubSchemasService/useDeleteSchema.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.ts'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import type { DataHubTableProps } from '@datahub/components/pages/DataHubListings.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import type { PolicySchemaExpanded } from '@datahub/utils/policy.utils'
import { groupResourceItems } from '@datahub/utils/policy.utils'

const SchemaTable: FC<DataHubTableProps> = ({ onDeleteItem }) => {
  const { t } = useTranslation('datahub')
  const { isLoading, data, isError } = useGetAllSchemas()
  const deleteSchema = useDeleteSchema()

  const expandedData = useMemo(() => {
    if (isLoading) return [mockSchemaTempHumidity]

    return groupResourceItems(data)
  }, [data, isLoading])

  const columns = useMemo<ColumnDef<PolicySchemaExpanded>[]>(() => {
    const onHandleDownload = (info: CellContext<PolicySchemaExpanded, unknown>) => () => {
      downloadJSON<PolicySchemaExpanded>(info.row.original.id, info.row.original)
    }

    return [
      {
        accessorKey: 'id',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text as="span" ml={info.row.getParentRow() ? 4 : 0}>
                {info.getValue<string>()}
              </Text>
            </Skeleton>
          )
        },
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
                onExpand={info.row.getToggleExpandedHandler()}
                canDownload={!info.row.getCanExpand()}
                canDelete={!info.row.getParentRow()}
                canExpand={info.row.getCanExpand()}
                isExpanded={info.row.getIsExpanded()}
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [deleteSchema.mutateAsync, isLoading, onDeleteItem, t])

  return (
    <PaginatedTable<PolicySchemaExpanded>
      aria-label={t('Listings.schema.label')}
      data={expandedData}
      columns={columns}
      isError={isError}
      getSubRows={(data) => data.children}
    />
  )
}

export default SchemaTable
