import type { FC } from 'react'
import { useMemo } from 'react'
import { Box, Skeleton } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'

import { AssetMapping } from '@/api/__generated__'
import { type ManagedAsset } from '@/api/__generated__'
import { MOCK_PULSE_ASSET } from '@/api/hooks/usePulse/__handlers__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { AssetActionMenu } from '@/modules/Pulse/components/assets/AssetActionMenu.tsx'

const AssetsTable: FC = () => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useListManagedAssets()

  const safeData = useMemo(() => {
    if (!data || !data?.items) return [MOCK_PULSE_ASSET]

    return data.items
  }, [data])

  const columns = useMemo<ColumnDef<ManagedAsset>[]>(() => {
    return [
      {
        accessorKey: 'name',
        header: t('pulse.assets.listing.column.name'),
        cell: (info) => {
          return <Skeleton isLoaded={!isLoading}>{info.getValue<string>()}</Skeleton>
        },
      },
      {
        accessorKey: 'description',
        header: t('pulse.assets.listing.column.name'),
        cell: (info) => {
          return <Skeleton isLoaded={!isLoading}>{info.getValue<string>()}</Skeleton>
        },
      },
      {
        accessorKey: 'topic',
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
        header: t('pulse.assets.listing.column.status'),
        cell: (info) => {
          return <Skeleton isLoaded={!isLoading}>{info.getValue<AssetMapping.status>()}</Skeleton>
        },
      },
      {
        id: 'actions',
        header: t('pulse.assets.listing.column.actions'),
        sortingFn: undefined,

        cell: (info) => {
          const bridge = info.row.original
          return (
            <Skeleton isLoaded={!isLoading}>
              <AssetActionMenu asset={bridge} />
            </Skeleton>
          )
        },
      },
    ]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading])

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
      columns={columns}
      enablePagination={true}
    />
  )
}

export default AssetsTable
