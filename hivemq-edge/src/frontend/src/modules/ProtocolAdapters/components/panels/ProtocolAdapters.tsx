import { FC, useMemo, useState } from 'react'
import { Box, HStack, Image, Skeleton, Text, useDisclosure, useTheme } from '@chakra-ui/react'
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
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import WorkspaceIcon from '@/components/Icons/WorkspaceIcon.tsx'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'

import { AdapterNavigateState, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import AdapterActionMenu from '../adapters/AdapterActionMenu.tsx'
import { compareStatus } from '../../utils/pagination-utils.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'

const DEFAULT_PER_PAGE = 10

const AdapterStatusContainer: FC<{ id: string }> = ({ id }) => {
  const { data: connections } = useGetAdaptersStatus()

  const connection = connections?.items?.find((e) => e.id === id)

  return <ConnectionStatusBadge status={connection} />
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

  const { data: allAdapters, isError: isErrorAllAdapters, error: errorAllAdapters } = useGetAdapterTypes()
  const { data: adapters, isLoading, isError, error } = useListProtocolAdapters()
  const [deleteAdapter, setDeleteAdapter] = useState<string | undefined>(undefined)
  const deleteProtocolAdapter = useDeleteProtocolAdapter()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()
  const { onDeleteNode } = useWorkspaceStore()

  const safeData: Adapter[] = adapters ? adapters : [mockAdapter, mockAdapter, mockAdapter, mockAdapter]

  const columns = useMemo<ColumnDef<Adapter>[]>(() => {
    const handleCreateInstance = (type: string | undefined) => {
      const adapterNavigateState: AdapterNavigateState = {
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
        protocolAdapterType: type,
      }
      navigate('/protocol-adapters/new', { state: adapterNavigateState })
    }

    const handleEditInstance = (adapterId: string, type: string) => {
      const adapterNavigateState: AdapterNavigateState = {
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
        protocolAdapterType: type,
      }
      if (adapterId) navigate(`/protocol-adapters/${adapterId}`, { state: adapterNavigateState })
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
        cell: (info) => {
          return <Skeleton isLoaded={!isLoading}>{info.row.original.id}</Skeleton>
        },
      },
      {
        accessorKey: 'type',
        cell: (info) => {
          const adapter = allAdapters?.items?.find((e) => e.id === info.row.original.type)
          return (
            <Skeleton isLoaded={!isLoading}>
              {adapter ? <AdapterTypeContainer {...adapter} /> : <>info.getValue()</>}
            </Skeleton>
          )
        },
        header: t('protocolAdapter.table.header.type') as string,
      },
      {
        accessorFn: (row) => row.status?.connection,
        id: 'status',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading}>
            <AdapterStatusContainer id={info.row.original.id} />
          </Skeleton>
        ),
        sortingFn: (rowA, rowB) => compareStatus(rowA.original.status?.connection, rowB.original.status?.connection),
      },
      {
        accessorFn: (row) => row.status?.startedAt,
        id: 'lastStartedAttemptTime',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading}>
            <DateTimeRenderer date={DateTime.fromISO(info.getValue() as string)} isApprox />
          </Skeleton>
        ),
        header: t('protocolAdapter.table.header.lastStarted') as string,
      },
      {
        id: 'actions',
        header: t('protocolAdapter.table.header.actions') as string,
        sortingFn: undefined,
        cell: (info) => {
          const { id, type } = info.row.original
          const { selectedActiveAdapter } = (state || {}) as AdapterNavigateState
          return (
            <Skeleton isLoaded={!isLoading}>
              <AdapterActionMenu
                adapter={info.row.original}
                onCreate={handleCreateInstance}
                onEdit={handleEditInstance}
                onDelete={handleOnDelete}
                onViewWorkspace={handleViewWorkspace}
              />
              {id === selectedActiveAdapter?.adapterId && (
                <IconButton
                  size={'sm'}
                  ml={2}
                  onClick={() => handleViewWorkspace(id, type as string)}
                  aria-label={t('bridge.subscription.delete')}
                  icon={<WorkspaceIcon />}
                />
              )}
            </Skeleton>
          )
        },
      },
    ]
  }, [t, navigate, onConfirmDeleteOpen, isLoading, allAdapters?.items, state])

  const handleConfirmOnClose = () => {
    onConfirmDeleteClose()
    setDeleteAdapter(undefined)
  }

  const handleConfirmOnSubmit = () => {
    if (!deleteAdapter) return

    deleteProtocolAdapter
      .mutateAsync(deleteAdapter)
      .then(() => {
        onDeleteNode(NodeTypes.ADAPTER_NODE, deleteAdapter)
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

  if (isError || isErrorAllAdapters) {
    return (
      <Box mt={'20%'} mx={'20%'} alignItems={'center'}>
        <ErrorMessage
          type={errorAllAdapters ? errorAllAdapters.message : error?.message}
          message={(error?.body as ProblemDetails)?.title || (t('protocolAdapter.error.loading') as string)}
        />
      </Box>
    )
  }

  if (safeData.length === 0)
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
        {!isLoading
          ? t('protocolAdapter.table.pagination.summary', {
              count: Math.min(DEFAULT_PER_PAGE, safeData.length),
              total: safeData.length,
            })
          : t('protocolAdapter.loading.activeAdapters')}
      </Text>
      <PaginatedTable<Adapter>
        aria-label={t('protocolAdapter.tabs.adapters')}
        data={safeData}
        columns={columns}
        getRowStyles={(row: Row<Adapter>) => {
          const { selectedActiveAdapter } = (state || {}) as AdapterNavigateState
          return row.original.id === selectedActiveAdapter?.adapterId ? { backgroundColor: colors.blue[50] } : {}
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
