import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { Box, Button, Flex, Icon, Skeleton, Text } from '@chakra-ui/react'
import { MdOutlineEventNote } from 'react-icons/md'
import { BiRefresh } from 'react-icons/bi'

import type { Event } from '@/api/__generated__'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'
import { useGetEvents } from '@/api/hooks/useEvents/useGetEvents.ts'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

import { compareSeverity } from '@/modules/ProtocolAdapters/utils/pagination-utils.ts'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'

import SourceLink from '../SourceLink.tsx'
import SeverityBadge from '../SeverityBadge.tsx'

interface EventLogTableProps {
  onOpen?: (t: Event) => void
  globalSourceFilter?: string[]
  maxEvents?: number
  variant?: 'full' | 'summary'
  isSingleSource?: boolean
}

const EventLogTable: FC<EventLogTableProps> = ({
  onOpen,
  globalSourceFilter,
  variant = 'full',
  maxEvents = 5,
  isSingleSource = false,
}) => {
  const { t } = useTranslation()
  const { data, isLoading, isFetching, error, refetch } = useGetEvents()

  const safeData = useMemo<Event[]>(() => {
    if (!data || !data?.items) return [...mockEdgeEvent(5)]
    if (globalSourceFilter) {
      return data.items
        .filter((e: Event) => globalSourceFilter.includes(e.source?.identifier || ''))
        .slice(0, maxEvents)
    }

    return data.items
  }, [data, globalSourceFilter, maxEvents])

  const allColumns = useMemo<ColumnDef<Event>[]>(() => {
    return [
      {
        accessorKey: 'identifier.identifier',
        width: '25%',
        enableSorting: false,
        enableColumnFilter: false,
        header: t('eventLog.table.header.id'),
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              {info.row.original.payload ? (
                <IconButton
                  size="sm"
                  mr={2}
                  onClick={() => onOpen?.(info.row.original)}
                  aria-label={t('eventLog.table.cta.open')}
                  icon={<MdOutlineEventNote />}
                />
              ) : null}
            </Skeleton>
          )
        },
      },
      {
        accessorKey: 'created',
        sortType: 'datetime',
        accessorFn: (row) => DateTime.fromISO(row.created).toMillis(),
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <DateTimeRenderer date={DateTime.fromMillis(info.getValue() as number)} isApprox />
          </Skeleton>
        ),
        header: t('eventLog.table.header.created'),
      },
      {
        accessorKey: 'severity',
        header: t('eventLog.table.header.severity'),
        sortingFn: (rowA, rowB) => compareSeverity(rowA.original.severity, rowB.original.severity),
        cell: (info) => (
          <Skeleton isLoaded={!isLoading}>
            <SeverityBadge event={info.row.original} />
          </Skeleton>
        ),
      },
      {
        accessorKey: 'source.identifier',
        sortType: 'alphanumeric',
        header: t('eventLog.table.header.source'),
        cell: (info) => (
          <Skeleton isLoaded={!isLoading}>
            <SourceLink source={info.row.original.source} type={info.row.original.associatedObject} />
          </Skeleton>
        ),
      },
      {
        accessorKey: 'message',
        header: t('eventLog.table.header.message'),
        enableColumnFilter: false,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} overflow="hidden">
              <Text>{info.getValue() as string}</Text>
            </Skeleton>
          )
        },
      },
    ]
  }, [isLoading, onOpen, t])

  const displayColumns = useMemo(() => {
    const [, createdColumn, severityColumn, idColumn, messageColumn] = allColumns
    if (variant === 'full') return allColumns
    if (isSingleSource) return [createdColumn, severityColumn, messageColumn]
    else return [createdColumn, idColumn, severityColumn, messageColumn]
  }, [allColumns, isSingleSource, variant])

  if (error) {
    return (
      <Box mt="20%" mx="20%" alignItems="center">
        <ErrorMessage
          type={error?.message}
          message={(error?.body as ProblemDetails)?.title || t('eventLog.error.loading')}
        />
      </Box>
    )
  }

  return (
    <>
      {variant === 'full' && (
        <Flex justifyContent="flex-end">
          <Button
            isLoading={isFetching}
            loadingText={t('eventLog.table.cta.refetch')}
            variant="outline"
            size="sm"
            leftIcon={<Icon as={BiRefresh} fontSize={20} />}
            onClick={() => refetch()}
          >
            {t('eventLog.table.cta.refetch')}
          </Button>
        </Flex>
      )}
      <PaginatedTable<Event>
        aria-label={t('eventLog.title')}
        data={safeData}
        columns={displayColumns}
        enablePaginationGoTo={variant === 'full'}
        enablePaginationSizes={variant === 'full'}
        enableColumnFilters={variant === 'full'}
        // getRowStyles={(row) => {
        //   return { backgroundColor: theme.colors.blue[50] }
        // }}
      />
    </>
  )
}

export default EventLogTable
