import type { FC } from 'react'
import { useMemo } from 'react'
import type { CellContext, ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { Skeleton, Text } from '@chakra-ui/react'

import type { Script } from '@/api/__generated__'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.ts'
import { useDeleteScript } from '@datahub/api/hooks/DataHubScriptsService/useDeleteScript.ts'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import type { DataHubTableProps } from '@datahub/components/pages/DataHubListings.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { downloadJSON } from '@/utils/download.utils.ts'

const ScriptTable: FC<DataHubTableProps> = ({ onDeleteItem }) => {
  const { t } = useTranslation('datahub')
  const { isLoading, data, isError } = useGetAllScripts({})
  const deleteScript = useDeleteScript()

  const safeData = useMemo<Script[]>(() => {
    if (isLoading) return [mockScript]

    return [...(data?.items || [])]
  }, [isLoading, data?.items])

  const columns = useMemo<ColumnDef<Script>[]>(() => {
    const onHandleDownload = (info: CellContext<Script, unknown>) => () => {
      downloadJSON<Script>(info.row.original.id, info.row.original)
    }

    return [
      {
        accessorKey: 'id',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <Text>{info.getValue<string>()}</Text>
          </Skeleton>
        ),
        header: t('Listings.script.header.id'),
      },
      {
        accessorKey: 'functionType',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>{t('resource.script.functionType', { context: info.getValue<string>() })}</Text>
            </Skeleton>
          )
        },
        header: t('Listings.script.header.functionType'),
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
        header: t('Listings.script.header.version'),
      },
      {
        accessorKey: 'description',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>{info.getValue<string>()}</Text>
            </Skeleton>
          )
        },
        header: t('Listings.script.header.description'),
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
        header: t('Listings.script.header.created'),
      },
      {
        id: 'actions',
        header: t('Listings.script.header.actions'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DataHubListAction
                onDelete={() =>
                  onDeleteItem?.(deleteScript.mutateAsync, DataHubNodeType.FUNCTION, info.row.original.id)
                }
                onDownload={onHandleDownload(info)}
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [deleteScript.mutateAsync, isLoading, onDeleteItem, t])

  return (
    <PaginatedTable<Script>
      aria-label={t('Listings.script.label')}
      data={safeData}
      columns={columns}
      isError={isError}
    />
  )
}

export default ScriptTable
