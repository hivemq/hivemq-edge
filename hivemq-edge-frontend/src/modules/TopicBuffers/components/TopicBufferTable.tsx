import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, Text } from '@chakra-ui/react'
import { LuPencil, LuTrash } from 'react-icons/lu'

import type { TopicBufferSubscription } from '@/api/__generated__'
import { useListTopicBufferSubscriptions } from '@/api/hooks/useTopicBuffers/index.ts'
import { useDeleteTopicBufferSubscription } from '@/api/hooks/useTopicBuffers/index.ts'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'

interface TopicBufferTableProps {
  onEdit: (item: TopicBufferSubscription) => void
}

const TopicBufferTable: FC<TopicBufferTableProps> = ({ onEdit }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError } = useListTopicBufferSubscriptions()
  const { mutate: deleteSubscription } = useDeleteTopicBufferSubscription()

  const columns = useMemo<ColumnDef<TopicBufferSubscription>[]>(
    () => [
      {
        accessorKey: 'topicFilter',
        header: t('topicBuffer.table.header.topicFilter'),
        cell: (info) => <Topic tagTitle={info.getValue<string>()} mr={3} />,
      },
      {
        accessorKey: 'maxMessages',
        header: t('topicBuffer.table.header.maxMessages'),
        cell: (info) => <Text>{info.getValue<number>()}</Text>,
      },
      {
        id: 'actions',
        header: t('topicBuffer.table.header.actions'),
        cell: (info) => (
          <ButtonGroup size="sm" role="toolbar">
            <IconButton
              aria-label={t('topicBuffer.action.edit.ariaLabel')}
              icon={<LuPencil />}
              onClick={() => onEdit(info.row.original)}
              data-testid="topic-buffer-edit"
            />
            <IconButton
              aria-label={t('topicBuffer.action.delete.ariaLabel')}
              icon={<LuTrash />}
              onClick={() => deleteSubscription({ topicFilter: info.row.original.topicFilter })}
              data-testid="topic-buffer-delete"
            />
          </ButtonGroup>
        ),
      },
    ],
    [deleteSubscription, onEdit, t]
  )

  if (isLoading) return <LoaderSpinner />
  if (isError) return <ErrorMessage message={t('topicBuffer.error.loading')} />

  return (
    <PaginatedTable<TopicBufferSubscription>
      aria-label={t('topicBuffer.title')}
      data={data?.items ?? []}
      columns={columns}
      enablePagination
    />
  )
}

export default TopicBufferTable
