import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { Alert, AlertIcon, AlertStatus, Box, IconButton, Skeleton, Text } from '@chakra-ui/react'
import { MdOutlineEventNote } from 'react-icons/md'

import { Event } from '@/api/__generated__'
import { useGetEvents } from '@/api/hooks/useEvents/useGetEvents.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { compareSeverity } from '@/modules/ProtocolAdapters/utils/pagination-utils.ts'
import SourceLink from './SourceLink.tsx'

interface EventLogTableProps {
  onOpen: (t: Event) => void
}

const EventLogTable: FC<EventLogTableProps> = ({ onOpen }) => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useGetEvents()

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
              <Box whiteSpace={'nowrap'}>
                <IconButton
                  size={'sm'}
                  mr={2}
                  onClick={() => onOpen(info.row.original)}
                  aria-label={t('eventLog.table.cta.open')}
                  icon={<MdOutlineEventNote />}
                />
              </Box>
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
        cell: (info) => {
          let status: AlertStatus = 'info'
          switch (info.row.original.severity) {
            case Event.severity.CRITICAL:
              status = 'error'
              break
            case Event.severity.ERROR:
              status = 'error'
              break
            case Event.severity.WARN:
              status = 'warning'
              break
            case Event.severity.INFO:
              status = 'info'
              break
          }

          return (
            <Skeleton isLoaded={!isLoading}>
              <Alert status={status}>
                <AlertIcon />
                {info.row.original.severity}
              </Alert>
            </Skeleton>
          )
        },
      },
      {
        accessorKey: 'associatedObject.identifier',
        sortType: 'alphanumeric',
        header: t('eventLog.table.header.source') as string,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <Box whiteSpace={'nowrap'}>{info.row.original.associatedObject?.identifier}</Box>
            </Skeleton>
          )
        },
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

  // TODO[NVL] Use Skeleton
  if (!data || !data.items || error) return null

  return (
    <PaginatedTable<Event>
      data={data.items}
      columns={columns}
      // getRowStyles={(row) => {
      //   return { backgroundColor: theme.colors.blue[50] }
      // }}
    />
  )
}

export default EventLogTable
