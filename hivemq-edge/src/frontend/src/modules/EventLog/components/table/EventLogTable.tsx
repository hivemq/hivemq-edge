import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { Box, Button, Flex, Icon, IconButton, Skeleton, Text } from '@chakra-ui/react'
import { MdOutlineEventNote } from 'react-icons/md'
import { BiRefresh } from 'react-icons/bi'

import { Event } from '@/api/__generated__'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'
import { useGetEvents } from '@/api/hooks/useEvents/useGetEvents.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

import { compareSeverity } from '@/modules/ProtocolAdapters/utils/pagination-utils.ts'

import SourceLink from '../SourceLink.tsx'
import SeverityBadge from '../SeverityBadge.tsx'

interface EventLogTableProps {
  onOpen: (t: Event) => void
}

const EventLogTable: FC<EventLogTableProps> = ({ onOpen }) => {
  const { t } = useTranslation()
  const { data, isLoading, isFetching, error, refetch } = useGetEvents()

  const safeData: Event[] = data && data.items ? data.items : [...mockEdgeEvent(5)]

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
                  onClick={() => onOpen(info.row.original)}
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
        accessorFn: (row) => DateTime.fromISO(row.created),
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace={'nowrap'}>
            {(info.getValue() as DateTime).toRelativeCalendar({ unit: 'minutes' })}
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

  return (
    <>
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
      <PaginatedTable<Event>
        data={safeData}
        columns={columns}
        enableColumnFilters
        // getRowStyles={(row) => {
        //   return { backgroundColor: theme.colors.blue[50] }
        // }}
      />
    </>
  )
}

export default EventLogTable
