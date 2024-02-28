import { FC, useMemo } from 'react'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Skeleton, Text } from '@chakra-ui/react'

import type { Script } from '@/api/__generated__'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.tsx'
import { useDeleteScript } from '@datahub/api/hooks/DataHubScriptsService/useDeleteScript.tsx'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'

const ScriptTable: FC = () => {
  const { t } = useTranslation('datahub')
  const { isLoading, data, isError } = useGetAllScripts({})
  const deleteScript = useDeleteScript()

  const navigate = useNavigate()

  const safeData = useMemo<Script[]>(() => {
    if (isLoading) return [mockScript]

    return [...(data?.items || [])]
  }, [isLoading, data?.items])

  const columns = useMemo<ColumnDef<Script>[]>(() => {
    return [
      {
        accessorKey: 'id',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <Text>{info.getValue<string>()}</Text>
          </Skeleton>
        ),
        header: t('Listings.script.header.id') as string,
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
        header: t('Listings.script.header.functionType') as string,
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
        header: t('Listings.script.header.version') as string,
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
        header: t('Listings.script.header.description') as string,
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
        header: t('Listings.script.header.created') as string,
      },
      {
        id: 'actions',
        header: t('Listings.script.header.actions') as string,
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DataHubListAction
                isEditDisabled={true}
                onDelete={() =>
                  deleteScript
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
    <PaginatedTable<Script>
      aria-label={t('Listings.script.label')}
      data={safeData}
      columns={columns}
      isError={isError}
    />
  )
}

export default ScriptTable
