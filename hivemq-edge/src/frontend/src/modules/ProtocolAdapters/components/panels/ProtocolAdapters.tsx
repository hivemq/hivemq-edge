import { FC, useMemo, useState } from 'react'
import { Box, Flex, HStack, IconButton, Image, Skeleton, Text, useDisclosure, useTheme } from '@chakra-ui/react'
import { ColumnDef, Row } from '@tanstack/react-table'
import { useTranslation } from 'react-i18next'
import { DateTime } from 'luxon'
import { useLocation, useNavigate } from 'react-router-dom'

import { Adapter, ApiError, ProtocolAdapter } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'
import { useDeleteProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useDeleteProtocolAdapter.tsx'
import { useGetAdaptersStatus } from '@/api/hooks/useConnection/useGetAdaptersStatus.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import WorkspaceIcon from '@/components/Icons/WorkspaceIcon.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import { compareStatus } from '../../utils/pagination-utils.ts'
import AdapterActionMenu from '../adapters/AdapterActionMenu.tsx'

const DEFAULT_PER_PAGE = 10

const AdapterStatusContainer: FC<{ id: string }> = ({ id }) => {
  const { data: connections } = useGetAdaptersStatus()

  const connection = connections?.items?.find((e) => e.id === id)

  return <ConnectionStatusBadge status={connection?.connectionStatus} />
}

const AdapterTypeContainer: FC<ProtocolAdapter> = (adapter) => {
  return (
    <HStack>
      <Image boxSize="30px" objectFit="scale-down" src={adapter.logoUrl} aria-label={adapter.id} />
      <Text fontSize={'md'} fontWeight={'500'}>
        {adapter.name}
      </Text>
    </HStack>
  )
}

const ProtocolAdapters: FC = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { state } = useLocation()
  const { successToast, errorToast } = useEdgeToast()
  const { colors } = useTheme()

  const { data: allAdapters } = useGetAdapterTypes()
  const { data, isLoading, isError, error } = useListProtocolAdapters()
  const isEmpty = useMemo(() => !data || data.length === 0, [data])
  const [deleteAdapter, setDeleteAdapter] = useState<string | undefined>(undefined)
  const deleteProtocolAdapter = useDeleteProtocolAdapter()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()

  const columns = useMemo<ColumnDef<Adapter>[]>(() => {
    const handleCreateInstance = (type: string | undefined) => {
      navigate('/protocol-adapters/new', { state: { selectedAdapterId: type } })
    }

    const handleEditInstance = (adapterId: string, type: string) => {
      if (adapterId) navigate(`/protocol-adapters/${adapterId}`, { state: { selectedAdapterId: type } })
    }

    const handleOnDelete = (adapterId: string) => {
      setDeleteAdapter(adapterId)
      onConfirmDeleteOpen()
    }

    const handleViewWorkspace = (adapterId: string, type: string) => {
      if (adapterId) navigate(`/edge-flow`, { state: { selectedAdapter: { adapterId, type } } })
    }

    return [
      {
        accessorKey: 'id',
        header: t('protocolAdapter.table.header.name') as string,
      },
      {
        accessorKey: 'type',
        cell: (info) => {
          const adapter = allAdapters?.items?.find((e) => e.id === info.row.original.type)
          return adapter ? <AdapterTypeContainer {...adapter} /> : info.getValue()
        },
        header: t('protocolAdapter.table.header.type') as string,
      },
      {
        accessorFn: (row) => row.runtimeStatus?.connectionStatus,
        id: 'status',
        cell: (info) => <AdapterStatusContainer id={info.row.original.id} />,
        sortingFn: (rowA, rowB) =>
          compareStatus(rowA.original.runtimeStatus?.connectionStatus, rowB.original.runtimeStatus?.connectionStatus),
      },
      {
        accessorFn: (row) => row.runtimeStatus?.startedAt,
        id: 'lastStartedAttemptTime',
        cell: (info) => DateTime.fromISO(info.getValue() as string).toRelativeCalendar({ unit: 'minutes' }),
        header: t('protocolAdapter.table.header.lastStarted') as string,
      },
      {
        id: 'actions',
        header: t('protocolAdapter.table.header.actions') as string,
        sortingFn: undefined,
        cell: (info) => {
          const { id, type } = info.row.original
          const { selectedAdapter } = state || {}
          return (
            <>
              <AdapterActionMenu
                adapter={info.row.original}
                onCreate={handleCreateInstance}
                onEdit={handleEditInstance}
                onDelete={handleOnDelete}
                onViewWorkspace={handleViewWorkspace}
              />
              {id === selectedAdapter?.adapterId && (
                <IconButton
                  size={'sm'}
                  ml={2}
                  onClick={() => handleViewWorkspace(id, type as string)}
                  aria-label={t('bridge.subscription.delete')}
                  icon={<WorkspaceIcon />}
                />
              )}
            </>
          )
        },
      },
    ]
  }, [state, navigate, onConfirmDeleteOpen, t, allAdapters?.items])

  const handleConfirmOnClose = () => {
    onConfirmDeleteClose()
    setDeleteAdapter(undefined)
  }

  const handleConfirmOnSubmit = () => {
    if (!deleteAdapter) return

    deleteProtocolAdapter
      .mutateAsync(deleteAdapter)
      .then(() => {
        successToast({
          title: t('protocolAdapter.toast.delete.title'),
          description: t('protocolAdapter.toast.delete.description'),
        })
      })
      .catch((err: ApiError) =>
        errorToast(
          {
            title: t('protocolAdapter.toast.delete.title'),
            description: t('protocolAdapter.toast.create.error'),
          },
          err
        )
      )
  }

  if (isError) {
    return (
      <Box mt={8}>
        <ErrorMessage type={error?.message} message={(error?.body as ProblemDetails)?.title} />
      </Box>
    )
  }

  if (isLoading) {
    return (
      <Flex flexDirection={'row'} flexWrap={'wrap'} gap={'20px'}>
        <Skeleton width={250} height={100}></Skeleton>
      </Flex>
    )
  }

  if (isEmpty || !data)
    return (
      <WarningMessage
        image={AdapterEmptyLogo}
        title={t('protocolAdapter.noDataWarning.title') as string}
        prompt={t('protocolAdapter.noDataWarning.description')}
        alt={t('protocolAdapter.title')}
        mt={10}
      />
    )

  return (
    <>
      <Text>
        {t('protocolAdapter.table.pagination.summary', {
          count: Math.min(DEFAULT_PER_PAGE, data.length),
          total: data.length,
        })}
      </Text>
      <PaginatedTable<Adapter>
        data={data}
        columns={columns}
        getRowStyles={(row: Row<Adapter>) => {
          const { selectedAdapter } = state || {}
          return row.original.id === selectedAdapter?.adapterId ? { backgroundColor: colors.blue[50] } : {}
        }}
      />

      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleConfirmOnClose}
        onSubmit={handleConfirmOnSubmit}
        message={t('modals.generics.confirmation')}
        header={t('modals.deleteProtocolAdapterDialog.header')}
      />
    </>
  )
}

export default ProtocolAdapters
