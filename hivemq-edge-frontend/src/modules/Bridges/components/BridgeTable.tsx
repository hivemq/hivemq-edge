import { type FC, useMemo } from 'react'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { Badge, Box, Skeleton } from '@chakra-ui/react'

import type { Bridge, BridgeSubscription, LocalBridgeSubscription, Status } from '@/api/__generated__'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'

import { BridgeActionMenu } from '@/modules/Bridges/components/BridgeActionMenu.tsx'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'

export const BridgeTable: FC = () => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useListBridges()

  const safeData = useMemo(() => {
    return data ? data : [mockBridge, mockBridge]
  }, [data])

  const columns = useMemo<ColumnDef<Bridge>[]>(() => {
    return [
      {
        accessorKey: 'id',
        cell: (info) => {
          return <Skeleton isLoaded={!isLoading}>{info.getValue<string>()}</Skeleton>
        },
      },
      // Might not need these fields
      // {
      //   accessorKey: 'host',
      // },
      // {
      //   accessorKey: 'port',
      // },
      {
        accessorKey: 'localSubscriptions',
        cell: (info) => {
          const val = info.getValue<Array<LocalBridgeSubscription>>()
          return (
            <Skeleton isLoaded={!isLoading}>
              <Badge variant="subtle">{val.length} </Badge>
            </Skeleton>
          )
        },
      },
      {
        accessorKey: 'remoteSubscriptions',
        cell: (info) => {
          const val = info.getValue<Array<BridgeSubscription>>()
          return (
            <Skeleton isLoaded={!isLoading}>
              <Badge variant="subtle">{val.length} </Badge>
            </Skeleton>
          )
        },
      },
      {
        accessorKey: 'status',
        cell: (info) => {
          const val = info.getValue<Status>()
          return (
            <Skeleton isLoaded={!isLoading}>
              <ConnectionStatusBadge status={val} />
            </Skeleton>
          )
        },
      },
      {
        accessorFn: (row) => row.status?.startedAt,
        id: 'lastStartedAttemptTime',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DateTimeRenderer date={DateTime.fromISO(info.getValue() as string)} isApprox />
            </Skeleton>
          )
        },
      },
      {
        id: 'actions',
        header: t('topicFilter.listing.column.action'),
        sortingFn: undefined,

        cell: (info) => {
          const bridge = info.row.original
          return (
            <Skeleton isLoaded={!isLoading}>
              <BridgeActionMenu
                bridge={bridge}
                // onCreate={handleCreateInstance}
                // onEdit={handleEditInstance}
                // onDelete={handleOnDelete}
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [isLoading, t])

  if (isError)
    return (
      <Box mt="20%" mx="20%" alignItems="center">
        <ErrorMessage
          type={error?.message}
          message={(error?.body as ProblemDetails)?.title || t('bridge.error.loading')}
        />
      </Box>
    )

  return (
    <PaginatedTable<Bridge>
      aria-label={t('topicFilter.listing.aria-label')}
      data={safeData}
      columns={columns}
      enablePagination={true}
    />
  )
}
