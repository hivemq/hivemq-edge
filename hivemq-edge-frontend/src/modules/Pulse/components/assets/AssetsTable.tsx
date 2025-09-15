import type { FC } from 'react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Box, Skeleton, Text, useDisclosure } from '@chakra-ui/react'
import type { ColumnDef } from '@tanstack/react-table'
import { chakraComponents } from 'chakra-react-select'
import debug from 'debug'

import type { ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import type { FilterMetadata } from '@/components/PaginatedTable/types.ts'
import { AssetActionMenu } from '@/modules/Pulse/components/assets/AssetActionMenu.tsx'
import AssetMapperWizard from '@/modules/Pulse/components/assets/AssetMapperWizard.tsx'
import AssetStatusBadge from '@/modules/Pulse/components/assets/AssetStatusBadge.tsx'
import FilteredCell from '@/modules/Pulse/components/assets/FilteredCell.tsx'
import SourcesCell from '@/modules/Pulse/components/assets/SourcesCell.tsx'
import { compareStatus } from '@/modules/Pulse/utils/pagination-utils.ts'
import type { WorkspaceNavigationCommand } from '@/modules/Workspace/types.ts'

const combinerLog = debug(`Combiner:AssetsTable`)

const skeletonTemplate: ManagedAsset = {
  id: ' ',
  name: ' ',
  description: ' ',
  topic: ' ',
  schema: ' ',
  mapping: { status: AssetMapping.status.UNMAPPED },
}

interface AssetTableProps {
  variant?: 'full' | 'summary'
}

interface SelectedAssetOperation {
  assetId: string
  asset?: ManagedAsset
  operation: 'DELETE' | 'EDIT'
}

const AssetsTable: FC<AssetTableProps> = ({ variant = 'full' }) => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useListManagedAssets()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [selectedAssetOperation, setSelectedAssetOperation] = useState<SelectedAssetOperation | undefined>(undefined)

  const safeData = useMemo(() => {
    if (!data?.items) return [skeletonTemplate, skeletonTemplate, skeletonTemplate]

    return data.items
  }, [data])

  const handleViewWorkspace = (adapterId: string, type: string, command: WorkspaceNavigationCommand) => {
    if (adapterId) navigate('/workspace', { state: { selectedAdapter: { adapterId, type, command } } })
  }

  const handleCloseWizard = () => {
    setSelectedAssetOperation(undefined)
    onClose()
  }

  const handleCloseDelete = () => {
    setSelectedAssetOperation(undefined)
    onClose()
  }

  const handleConfirmDelete = () => {
    setSelectedAssetOperation(undefined)
    onClose()
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
        accessorFn: (row) => row.mapping.status,
        accessorKey: 'mapping.status',
        enableGlobalFilter: false,
        header: t('pulse.assets.listing.column.status'),
        sortingFn: (rowA, rowB) => compareStatus(rowA.original.mapping.status, rowB.original.mapping.status),
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
            components: {
              Option: ({ children, ...props }) => (
                <chakraComponents.Option {...props}>
                  <AssetStatusBadge status={children as AssetMapping.status} />
                </chakraComponents.Option>
              ),
              SingleValue: ({ children, ...props }) => (
                <chakraComponents.SingleValue {...props}>
                  <AssetStatusBadge status={children as AssetMapping.status} />
                </chakraComponents.SingleValue>
              ),
            },
          },
        } as FilterMetadata,
      },
      {
        // accessorFn: (row) => row.mapping.sources ?? [],
        accessorKey: 'combiner.sources',
        enableGlobalFilter: false,
        enableColumnFilter: false,
        enableSorting: false,
        sortingFn: undefined,
        // filterFn: (row, _, filterValue) => {
        //   return !!row.original.mapping.sources.some((e) => e.id === filterValue)
        // },
        header: t('pulse.assets.listing.column.sources'),
        cell: (info) => {
          const mappingId = info.row.original.mapping.mappingId
          return (
            <Skeleton isLoaded={!isLoading}>
              {!mappingId && <Text whiteSpace="nowrap">{t('pulse.assets.listing.sources.unset')}</Text>}
              {mappingId && <SourcesCell mappingId={mappingId} isLoading={isLoading} />}
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
              <AssetActionMenu
                asset={asset}
                onViewWorkspace={handleViewWorkspace}
                isInWorkspace={variant === 'summary'}
                onView={(id) => navigate(`/pulse-assets/${id}`)}
                onEdit={(id) => {
                  const asset = data?.items.find((e) => e.id === id)
                  if (asset) {
                    setSelectedAssetOperation({ assetId: id, operation: 'EDIT', asset })
                    onOpen()
                  } else combinerLog('Cannot find the asset')
                }}
                onDelete={(id) => {
                  const asset = data?.items.find((e) => e.id === id)
                  if (asset) {
                    setSelectedAssetOperation({ assetId: id, operation: 'DELETE', asset })
                    onOpen()
                  } else combinerLog('Cannot find the asset')
                }}
              />
            </Skeleton>
          )
        },
      },
    ]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading])

  // TODO[NVL] Tanstack table has a column-visibility as a state; manage it  through it?
  const dynamicColumns = useMemo(() => {
    const [name, , topic, status, , actions] = columns

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
    <>
      <PaginatedTable<ManagedAsset>
        aria-label={t('pulse.assets.listing.aria-label')}
        noDataText={t('pulse.assets.listing.noDataText')}
        data={safeData}
        columns={dynamicColumns}
        enablePagination
        enableColumnFilters
        enableGlobalFilter
        initState={{ columnFilters: [{ id: 'mapping_status', value: searchParams.get('mapping_status') || '' }] }}
      />
      {selectedAssetOperation && (
        <>
          <AssetMapperWizard
            assetId={selectedAssetOperation?.assetId}
            onClose={handleCloseWizard}
            isOpen={isOpen && selectedAssetOperation?.operation === 'EDIT'}
          />
          <ConfirmationDialog
            isOpen={isOpen && selectedAssetOperation?.operation === 'DELETE'}
            onClose={handleCloseDelete}
            onSubmit={handleConfirmDelete}
            header={t('pulse.assets.operation.delete.header')}
            message={t('pulse.assets.operation.delete.message', { name: selectedAssetOperation?.asset?.name })}
          />
        </>
      )}
    </>
  )
}

export default AssetsTable
