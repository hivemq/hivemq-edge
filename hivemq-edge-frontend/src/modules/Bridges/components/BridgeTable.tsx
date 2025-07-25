import { type FC, useMemo, useState } from 'react'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { Badge, Box, Skeleton, useDisclosure } from '@chakra-ui/react'
import { useNavigate } from 'react-router-dom'

import type { ApiError, Bridge, BridgeSubscription, LocalBridgeSubscription } from '@/api/__generated__'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { useDeleteBridge } from '@/api/hooks/useGetBridges/useDeleteBridge.ts'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import { BridgeActionMenu } from '@/modules/Bridges/components/BridgeActionMenu.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { BridgeStatusContainer } from '@/modules/Bridges/components/BridgeStatusContainer.tsx'

export const BridgeTable: FC = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { data, isLoading, isError, error } = useListBridges()
  const [deleteBridge, setDeleteBridge] = useState<string | undefined>(undefined)
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()
  const { successToast, errorToast } = useEdgeToast()
  const mutateDeleteBridge = useDeleteBridge()
  const { onDeleteNode } = useWorkspaceStore()

  const safeData = useMemo(() => {
    return data ? data : [mockBridge, mockBridge]
  }, [data])

  const columns = useMemo<ColumnDef<Bridge>[]>(() => {
    const handleOnDelete = (bridgeId: string) => {
      setDeleteBridge(bridgeId)
      onConfirmDeleteOpen()
    }

    const handleOnEdit = (bridgeId: string) => {
      if (!bridgeId) return

      navigate(`/mqtt-bridges/${bridgeId}`)
    }

    return [
      {
        accessorKey: 'id',
        header: t('bridge.listing.column.id'),
        cell: (info) => {
          return <Skeleton isLoaded={!isLoading}>{info.getValue<string>()}</Skeleton>
        },
      },
      {
        accessorKey: 'localSubscriptions',
        header: t('bridge.listing.column.subscriptions', { context: 'local' }),
        cell: (info) => {
          const val = info.getValue<Array<LocalBridgeSubscription>>()
          return (
            <Skeleton isLoaded={!isLoading}>
              <Badge variant="subtle">{val.length}</Badge>
            </Skeleton>
          )
        },
      },
      {
        accessorKey: 'remoteSubscriptions',
        header: t('bridge.listing.column.subscriptions', { context: 'remote' }),
        cell: (info) => {
          const val = info.getValue<Array<BridgeSubscription>>()
          return (
            <Skeleton isLoaded={!isLoading}>
              <Badge variant="subtle">{val.length}</Badge>
            </Skeleton>
          )
        },
      },
      {
        accessorKey: 'status',
        header: t('bridge.listing.column.status'),
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <BridgeStatusContainer id={info.row.original.id} />
            </Skeleton>
          )
        },
      },
      {
        accessorFn: (row) => row.status?.startedAt,
        header: t('bridge.listing.column.lastStarted'),
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
        header: t('bridge.listing.column.actions'),
        sortingFn: undefined,

        cell: (info) => {
          const bridge = info.row.original
          return (
            <Skeleton isLoaded={!isLoading}>
              <BridgeActionMenu bridge={bridge} onEdit={handleOnEdit} onDelete={handleOnDelete} />
            </Skeleton>
          )
        },
      },
    ]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading])

  const handleConfirmOnClose = () => {
    onConfirmDeleteClose()
    setDeleteBridge(undefined)
  }

  const handleConfirmOnSubmit = () => {
    if (!deleteBridge) return

    mutateDeleteBridge
      .mutateAsync(deleteBridge)
      .then(() => {
        // TODO[NVL] This is not clearing the host node
        onDeleteNode(NodeTypes.BRIDGE_NODE, deleteBridge)
        successToast({
          title: t('bridge.toast.delete.title'),
          description: t('bridge.toast.delete.description'),
        })
      })
      .catch((err: ApiError) =>
        errorToast(
          {
            title: t('bridge.toast.delete.title'),
            description: t('bridge.toast.create.error'),
          },
          err
        )
      )
  }

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
    <>
      <PaginatedTable<Bridge>
        aria-label={t('bridge.listing.aria-label')}
        noDataText={t('bridge.listing.noDataText')}
        data={safeData}
        columns={columns}
        enablePagination={true}
      />
      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleConfirmOnClose}
        onSubmit={handleConfirmOnSubmit}
        message={t('modals.generics.confirmation')}
        header={t('modals.deleteBridgeDialog.header')}
      />
    </>
  )
}
