import type { FC } from 'react'
import { useMemo, useState } from 'react'
import {
  Box,
  Flex,
  Heading,
  HStack,
  Image,
  Skeleton,
  Text,
  useColorModeValue,
  useDisclosure,
  useToken,
} from '@chakra-ui/react'
import type { ColumnDef, Row } from '@tanstack/react-table'
import { useTranslation } from 'react-i18next'
import { DateTime } from 'luxon'
import { useLocation, useNavigate } from 'react-router-dom'

import type { Adapter, ApiError, ProtocolAdapter } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { useDeleteProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useDeleteProtocolAdapter.ts'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { WorkspaceIcon } from '@/components/Icons/TopicIcon.tsx'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'

import type { AdapterNavigateState } from '@/modules/ProtocolAdapters/types.ts'
import { ProtocolAdapterTabIndex, WorkspaceAdapterCommand } from '@/modules/ProtocolAdapters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import AdapterActionMenu from '../adapters/AdapterActionMenu.tsx'
import { compareStatus } from '../../utils/pagination-utils.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import { AdapterStatusContainer } from '@/modules/ProtocolAdapters/components/adapters/AdapterStatusContainer.tsx'

const DEFAULT_PER_PAGE = 10

const AdapterTypeContainer: FC<ProtocolAdapter> = (adapter) => {
  return (
    <HStack>
      <Image boxSize="30px" objectFit="scale-down" src={adapter.logoUrl} aria-label={adapter.id} />
      <Text fontSize="md" fontWeight="500">
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
  const selectedActiveAdapterColor = useToken('colors', useColorModeValue('blue.50', 'blue.900'))

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
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.ADAPTERS,
        protocolAdapterType: type,
      }
      navigate(`/protocol-adapters/catalog/new/${type}`, { state: adapterNavigateState })
    }

    const handleEditInstance = (adapterId: string, type: string) => {
      const adapterNavigateState: AdapterNavigateState = {
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.ADAPTERS,
        protocolAdapterType: type,
      }
      if (adapterId) navigate(`/protocol-adapters/edit/${type}/${adapterId}`, { state: adapterNavigateState })
    }

    const handleOnDelete = (adapterId: string) => {
      setDeleteAdapter(adapterId)
      onConfirmDeleteOpen()
    }

    const handleViewWorkspace = (adapterId: string, type: string, command: WorkspaceAdapterCommand) => {
      if (adapterId) navigate(`/workspace`, { state: { selectedAdapter: { adapterId, type, command } } })
    }

    const handleExport = (adapterId: string, type: string) => {
      if (adapterId) navigate(`/protocol-adapters/edit/${type}/${adapterId}/export`)
    }

    return [
      {
        accessorKey: 'id',
        header: t('protocolAdapter.table.header.name'),
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
        header: t('protocolAdapter.table.header.type'),
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
        header: t('protocolAdapter.table.header.lastStarted'),
      },
      {
        id: 'actions',
        header: t('protocolAdapter.table.header.actions'),
        sortingFn: undefined,
        cell: (info) => {
          const { id, type } = info.row.original
          const { selectedActiveAdapter } = (state || {}) as AdapterNavigateState
          const protocol = allAdapters?.items?.find((e) => e.id === info.row.original.type)
          return (
            <Skeleton isLoaded={!isLoading}>
              <AdapterActionMenu
                protocol={protocol}
                adapter={info.row.original}
                onCreate={handleCreateInstance}
                onEdit={handleEditInstance}
                onDelete={handleOnDelete}
                onViewWorkspace={handleViewWorkspace}
                onExport={handleExport}
              />
              {id === selectedActiveAdapter?.adapterId && (
                <IconButton
                  size="sm"
                  ml={2}
                  onClick={() => handleViewWorkspace(id, type as string, WorkspaceAdapterCommand.VIEW)}
                  aria-label={t('protocolAdapter.table.actions.workspace.view')}
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
      <Box mt="20%" mx="20%" alignItems="center">
        <ErrorMessage
          type={errorAllAdapters ? errorAllAdapters.message : error?.message}
          message={(error?.body as ProblemDetails)?.title || t('protocolAdapter.error.loading')}
        />
      </Box>
    )
  }

  if (safeData.length === 0)
    return (
      <WarningMessage
        image={AdapterEmptyLogo}
        title={t('protocolAdapter.noDataWarning.title')}
        prompt={t('protocolAdapter.noDataWarning.description')}
        alt={t('protocolAdapter.title')}
        mt={10}
      />
    )

  return (
    <Flex flexDirection="column" gap={4}>
      <Box data-testid="heading-adapters-list">
        <Heading size="md">{t('protocolAdapter.tabs.adapters')}</Heading>
        <Text>
          {!isLoading
            ? t('protocolAdapter.table.pagination.summary', {
                count: Math.min(DEFAULT_PER_PAGE, safeData.length),
                total: safeData.length,
              })
            : t('protocolAdapter.loading.activeAdapters')}
        </Text>
      </Box>
      <PaginatedTable<Adapter>
        aria-label={t('protocolAdapter.tabs.adapters')}
        data={safeData}
        columns={columns}
        getRowStyles={(row: Row<Adapter>) => {
          const { selectedActiveAdapter } = (state || {}) as AdapterNavigateState
          return row.original.id === selectedActiveAdapter?.adapterId
            ? { backgroundColor: selectedActiveAdapterColor }
            : {}
        }}
      />

      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleConfirmOnClose}
        onSubmit={handleConfirmOnSubmit}
        message={t('modals.generics.confirmation')}
        header={t('modals.deleteProtocolAdapterDialog.header')}
      />
      <SuspenseOutlet />
    </Flex>
  )
}

export default ProtocolAdapters
