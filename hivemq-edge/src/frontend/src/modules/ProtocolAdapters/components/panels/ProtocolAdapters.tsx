import { FC, useMemo, useState } from 'react'
import {
  Box,
  Flex,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  Image,
  Skeleton,
  Text,
  IconButton,
  useDisclosure,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { DateTime } from 'luxon'
import { Table, createColumn } from 'react-chakra-pagination'
import { ChevronDownIcon } from '@chakra-ui/icons'
import { useNavigate } from 'react-router-dom'

import { ApiError, ConnectionStatus } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'
import { useDeleteProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useDeleteProtocolAdapter.tsx'
import { useGetAdaptersStatus } from '@/api/hooks/useConnection/useGetAdaptersStatus.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

const DEFAULT_PER_PAGE = 10

const ProtocolAdapters: FC = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { successToast, errorToast } = useEdgeToast()

  const { data, isLoading, isError, error } = useListProtocolAdapters()
  const isEmpty = useMemo(() => !data || data.length === 0, [data])
  const [deleteAdapter, setDeleteAdapter] = useState<string | undefined>(undefined)
  const deleteProtocolAdapter = useDeleteProtocolAdapter()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()
  const { data: connections } = useGetAdaptersStatus()

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

  const columnHelper = createColumn<(typeof data)[0]>()
  const columns = [
    columnHelper.accessor('id', {
      cell: (info) => info.getValue(),
      header: t('protocolAdapter.table.header.name') as string,
    }),
    columnHelper.accessor('type', {
      cell: (info) => info.getValue(),
      header: t('protocolAdapter.table.header.type') as string,
    }),
    columnHelper.accessor('adapterRuntimeInformation.connectionStatus.status', {
      cell: (info) => {
        const { id } = info.row.original
        const connection = connections?.items?.find((e) => e.id === id)
        return <ConnectionStatusBadge status={connection?.status} />
      },
      header: t('protocolAdapter.table.header.status') as string,
    }),
    columnHelper.accessor('adapterRuntimeInformation.lastStartedAttemptTime', {
      cell: (info) => DateTime.fromISO(info.getValue() as string).toRelativeCalendar({ unit: 'minutes' }),
      header: t('protocolAdapter.table.header.lastStarted') as string,
      enableSorting: true,
    }),
    columnHelper.display({
      id: 'actions',
      header: t('protocolAdapter.table.header.actions') as string,
      sortingFn: undefined,
      cell: (info) => {
        const { type, id, adapterRuntimeInformation: { connectionStatus } = {} } = info.row.original
        return (
          <Menu>
            <MenuButton
              variant="outline"
              size={'sm'}
              as={IconButton}
              icon={<ChevronDownIcon />}
              aria-label={t('protocolAdapter.table.actions.label') as string}
            />
            <MenuList>
              <MenuItem isDisabled>
                {connectionStatus?.status !== ConnectionStatus.status.CONNECTED
                  ? t('protocolAdapter.table.actions.connect')
                  : t('protocolAdapter.table.actions.disconnect')}
              </MenuItem>
              <MenuItem onClick={() => handleCreateInstance(type)}>
                {t('protocolAdapter.table.actions.create')}
              </MenuItem>
              <MenuItem onClick={() => handleEditInstance(id, type as string)}>
                {t('protocolAdapter.table.actions.edit')}
              </MenuItem>
              <MenuItem color={'red.500'} onClick={() => handleOnDelete(id)}>
                <Text>{t('protocolAdapter.table.actions.delete')}</Text>
              </MenuItem>
            </MenuList>
          </Menu>
        )
      },
    }),
  ]

  // TODO[NVL] Lib not very customisable; redo
  return (
    <>
      <Text>
        {t('protocolAdapter.table.pagination.summary', {
          count: Math.min(DEFAULT_PER_PAGE, data.length),
          total: data.length,
        })}
      </Text>
      <Table
        // Fallback component when list is empty
        emptyData={{
          icon: (
            <Image
              objectFit="cover"
              maxW={{ base: '100%', md: '200px' }}
              src={AdapterEmptyLogo}
              alt={t('bridge.noDataWarning.title') as string}
            />
          ),
          text: t('bridge.noDataWarning.title') as string,
        }}
        itemsPerPage={DEFAULT_PER_PAGE}
        // totalRegisters={1}
        // onPageChange={(page) => console.log(page)}
        columns={columns}
        data={data}
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
