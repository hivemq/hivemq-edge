import { type FC, useMemo } from 'react'
import type { CellContext, ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { HStack, Skeleton, Text } from '@chakra-ui/react'

import type { Script, ScriptList } from '@/api/__generated__'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { downloadJSON } from '@/utils/download.utils.ts'

import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.ts'
import { useDeleteScript } from '@datahub/api/hooks/DataHubScriptsService/useDeleteScript.ts'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import { ExpandVersionButton } from '@datahub/components/helpers/ExpandVersionButton.tsx'
import type { DataHubTableProps } from '@datahub/components/pages/DataHubListings.tsx'
import { groupResourceItems, type ExpandableGroupedResource } from '@datahub/utils/policy.utils.ts'
import { DataHubNodeType } from '@datahub/types.ts'

const ScriptTable: FC<DataHubTableProps> = ({ onDeleteItem }) => {
  const { t } = useTranslation('datahub')
  const { isLoading, data, isError } = useGetAllScripts({})
  const deleteScript = useDeleteScript()

  const expandedData = useMemo(() => {
    if (isLoading) return [mockScript]

    return groupResourceItems<ScriptList, Script>(data)
  }, [data, isLoading])

  const columns = useMemo<ColumnDef<Script>[]>(() => {
    const onHandleDownload = (info: CellContext<Script, unknown>) => () => {
      downloadJSON<Script>(info.row.original.id, info.row.original)
    }

    return [
      {
        accessorKey: 'id',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap" as={HStack} justifyContent="space-between">
            <Text as="span" ml={info.row.getParentRow() ? 4 : 0}>
              {info.getValue<string>()}
            </Text>
            {info.row.getCanExpand() && (
              <ExpandVersionButton
                onClick={info.row.getToggleExpandedHandler()}
                isExpanded={info.row.getIsExpanded()}
              />
            )}
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
          const value = info.getValue<number>()
          const formattedValue = info.row.getCanExpand()
            ? t('Listings.resources.versions', { context: 'COUNT', count: value })
            : value
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>{formattedValue}</Text>
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
                canDownload={!info.row.getCanExpand()}
                canDelete={!info.row.getParentRow()}
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [deleteScript.mutateAsync, isLoading, onDeleteItem, t])

  return (
    <PaginatedTable<ExpandableGroupedResource<Script>>
      aria-label={t('Listings.script.label')}
      data={expandedData}
      columns={columns}
      isError={isError}
      getSubRows={(data) => data.children}
    />
  )
}

export default ScriptTable
