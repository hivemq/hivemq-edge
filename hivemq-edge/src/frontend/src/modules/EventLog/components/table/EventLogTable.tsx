import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { Box, Button, Flex, Icon, Skeleton, Text } from '@chakra-ui/react'
import { MdOutlineEventNote } from 'react-icons/md'
import { BiRefresh } from 'react-icons/bi'

import { Event } from '@/api/__generated__'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'
import { useGetEvents } from '@/api/hooks/useEvents/useGetEvents.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

import { compareSeverity } from '@/modules/ProtocolAdapters/utils/pagination-utils.ts'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'

import SourceLink from '../SourceLink.tsx'
import SeverityBadge from '../SeverityBadge.tsx'

interface EventLogTableProps {
  onOpen?: (t: Event) => void
  globalSourceFilter?: string
  variant?: 'full' | 'summary'
}

const EventLogTable: FC<EventLogTableProps> = ({ onOpen, globalSourceFilter, variant = 'full' }) => {
  const { t } = useTranslation()
  const { data, isLoading, isFetching, error, refetch } = useGetEvents()

  const safeData = useMemo<Event[]>(() => {
    if (!data || !data?.items) return [...mockEdgeEvent(5)]
    if (globalSourceFilter) {
      return data.items.filter((e: Event) => e.source?.identifier === globalSourceFilter).slice(0, 5)
    }

    return data.items
  }, [data, globalSourceFilter])

  const columns = useMemo<ColumnDef<Event>[]>(() => {
    return [
      {
        accessorKey: 'identifier.identifier',
        width: '25%',
        enableSorting: false,
        enableColumnFilter: false,
        header: t('eventLog.table.header.id') as string,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              {info.row.original.payload ? (
                <IconButton
                  size={'sm'}
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
          <Skeleton isLoaded={!isLoading} whiteSpace={'nowrap'}>
            <DateTimeRenderer date={DateTime.fromMillis(info.getValue() as number)} isApprox />
          </Skeleton>
        ),
        header: t('eventLog.table.header.created') as string,
      },
      {
        accessorKey: 'severity',
        header: t('eventLog.table.header.severity') as string,
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
        header: t('eventLog.table.header.source') as string,
        cell: (info) => (
          <Skeleton isLoaded={!isLoading}>
            <SourceLink source={info.row.original.source} type={info.row.original.associatedObject} />
          </Skeleton>
        ),
      },
      {
        accessorKey: 'message',
        header: t('eventLog.table.header.message') as string,
        enableColumnFilter: false,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} overflow={'hidden'}>
              <Text>{info.getValue() as string}</Text>
            </Skeleton>
          )
        },
      },
    ]
  }, [isLoading, onOpen, t])

  if (error) {
    return (
      <Box mt={'20%'} mx={'20%'} alignItems={'center'}>
        <ErrorMessage
          type={error?.message}
          message={(error?.body as ProblemDetails)?.title || (t('eventLog.error.loading') as string)}
        />
      </Box>
    )
  }

  // TODO[NVL] Not the best approach; destructure within memo
  const [, a, b, , c] = columns

  return (
    <>
      {variant === 'full' && (
        <Flex justifyContent={'flex-end'}>
          <Button
            isLoading={isFetching}
            loadingText={t('eventLog.table.cta.refetch')}
            variant={'outline'}
            size={'sm'}
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
        columns={variant === 'full' ? columns : [a, b, c]}
        enablePagination={variant === 'full'}
        enableColumnFilters={variant === 'full'}
        // getRowStyles={(row) => {
        //   return { backgroundColor: theme.colors.blue[50] }
        // }}
      />
    </>
  )
}

export default EventLogTable
