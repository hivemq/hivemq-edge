import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { Box, IconButton, Skeleton, Text } from '@chakra-ui/react'
import { MdOutlineEventNote } from 'react-icons/md'

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
  const { data, isLoading, error } = useGetEvents()

  const safeData: Event[] = data && data.items ? data.items : [...mockEdgeEvent(5)]

  const columns = useMemo<ColumnDef<Event>[]>(() => {
    return [
      {
        accessorKey: 'identifier.identifier',
        width: '25%',
        enableSorting: false,
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
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace={'nowrap'}>
            {DateTime.fromISO(info.getValue() as string).toRelativeCalendar({ unit: 'minutes' })}
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
        accessorKey: 'associatedObject.identifier',
        sortType: 'alphanumeric',
        header: t('eventLog.table.header.source') as string,
        cell: (info) => (
          <Skeleton isLoaded={!isLoading}>
            <SourceLink event={info.row.original.source} />
          </Skeleton>
        ),
      },
      {
        accessorKey: 'message',
        header: t('eventLog.table.header.message') as string,
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
    <PaginatedTable<Event>
      data={safeData}
      columns={columns}
      // getRowStyles={(row) => {
      //   return { backgroundColor: theme.colors.blue[50] }
      // }}
    />
  )
}

export default EventLogTable
