import type { FC } from 'react'
import { useMemo } from 'react'
import { Box, Skeleton } from '@chakra-ui/react'
import type { ColumnDef } from '@tanstack/react-table'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import type { ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import type { FilterMetadata } from '@/components/PaginatedTable/types.ts'
import { AssetActionMenu } from '@/modules/Pulse/components/assets/AssetActionMenu.tsx'
import AssetStatusBadge from '@/modules/Pulse/components/assets/AssetStatusBadge.tsx'
import FilteredCell from '@/modules/Pulse/components/assets/FilteredCell.tsx'
import SourcesCell from '@/modules/Pulse/components/assets/SourcesCell.tsx'
import { compareStatus } from '@/modules/Pulse/utils/pagination-utils.ts'
import type { WorkspaceNavigationCommand } from '@/modules/Workspace/types.ts'

const skeletonTemplate: ManagedAsset = {
  id: ' ',
  name: ' ',
  description: ' ',
  topic: ' ',
  schema: ' ',
}

interface AssetTableProps {
  variant?: 'full' | 'summary'
}

const AssetsTable: FC<AssetTableProps> = ({ variant = 'full' }) => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useListManagedAssets()
  const navigate = useNavigate()

  const safeData = useMemo(() => {
    if (!data || !data?.items) return [skeletonTemplate, skeletonTemplate, skeletonTemplate]

    return data.items
  }, [data])

  const handleViewWorkspace = (adapterId: string, type: string, command: WorkspaceNavigationCommand) => {
    if (adapterId) navigate(`/workspace`, { state: { selectedAdapter: { adapterId, type, command } } })
  }

  const columns = useMemo<ColumnDef<ManagedAsset>[]>(() => {
    return [
      {
        accessorKey: 'name',
        enableGlobalFilter: true,
        enableColumnFilter: false,
        header: t('pulse.assets.listing.column.name'),
        cell: (info) => (
          <FilteredCell
            isLoading={isLoading}
            value={info.getValue<string>()}
            canGlobalFilter={info.column.getCanGlobalFilter()}
            globalFilter={info.table.getState().globalFilter}
          />
        ),
      },
      {
        accessorKey: 'description',
        enableGlobalFilter: true,
        enableColumnFilter: false,
        header: t('pulse.assets.listing.column.description'),
        cell: (info) => (
          <FilteredCell
            isLoading={isLoading}
            value={info.getValue<string>()}
            canGlobalFilter={info.column.getCanGlobalFilter()}
            globalFilter={info.table.getState().globalFilter}
          />
        ),
      },
      {
        accessorKey: 'topic',
        enableGlobalFilter: false,
        header: t('pulse.assets.listing.column.topic'),
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <Topic tagTitle={info.getValue<string>()} />
            </Skeleton>
          )
        },
      },
      {
        accessorFn: (row) => row.mapping?.status ?? AssetMapping.status.UNMAPPED,
        accessorKey: 'mapping.status',
        enableGlobalFilter: false,
        header: t('pulse.assets.listing.column.status'),
        sortingFn: (rowA, rowB) => compareStatus(rowA.original.mapping?.status, rowB.original.mapping?.status),
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <AssetStatusBadge status={info.getValue<AssetMapping.status>()} />
            </Skeleton>
          )
        },
        meta: {
          filterOptions: {
            canCreate: false,
          },
        } as FilterMetadata,
      },
      {
        accessorFn: (row) => row.mapping?.sources ?? [],
        accessorKey: 'mapping.sources',
        enableGlobalFilter: false,
        enableColumnFilter: true,
        enableSorting: false,
        sortingFn: undefined,
        filterFn: (row, _, filterValue) => {
          return !!row.original.mapping?.sources.some((e) => e.id === filterValue)
        },
        header: t('pulse.assets.listing.column.sources'),
        cell: (info) => {
          const { sources, primary } = info.row.original.mapping || {}
          return (
            <Skeleton isLoaded={!isLoading}>
              <SourcesCell sources={sources} primary={primary} />
            </Skeleton>
          )
        },
      },
      {
        id: 'actions',
        enableGlobalFilter: false,
        header: t('pulse.assets.listing.column.actions'),
        sortingFn: undefined,

        cell: (info) => {
          const asset = info.row.original
          return (
            <Skeleton isLoaded={!isLoading}>
              <AssetActionMenu asset={asset} onViewWorkspace={handleViewWorkspace} />
            </Skeleton>
          )
        },
      },
    ]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading])

  // TODO[NVL] Tanstack table has a column-visibility as a state; manage it  through it?
  const dynamicColumns = useMemo(() => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const [name, _description, topic, status, _sources, actions] = columns

    return variant === 'full' ? columns : [name, topic, status, actions]
  }, [columns, variant])

  if (error) {
    return (
      <Box mt="20%" mx="20%" alignItems="center">
        <ErrorMessage
          type={error?.message}
          message={(error?.body as ProblemDetails)?.title || t('pulse.error.loading')}
        />
      </Box>
    )
  }

  return (
    <PaginatedTable<ManagedAsset>
      aria-label={t('pulse.assets.listing.aria-label')}
      noDataText={t('pulse.assets.listing.noDataText')}
      data={safeData}
      columns={dynamicColumns}
      enablePagination
      enableColumnFilters
      enableGlobalFilter
    />
  )
}

export default AssetsTable
