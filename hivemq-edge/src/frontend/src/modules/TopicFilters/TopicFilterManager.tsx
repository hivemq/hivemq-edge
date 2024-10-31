import { type FC, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { Button, ButtonGroup, Card, CardBody, Text, useDisclosure } from '@chakra-ui/react'
import { LuPencil, LuPlus, LuTrash, LuView } from 'react-icons/lu'

import type { TopicFilter } from '@/api/__generated__'
import { useTopicFilterOperations } from '@/api/hooks/useTopicFilters/useTopicFilterOperations.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import ArrayItemDrawer from '@/components/rjsf/SplitArrayEditor/components/ArrayItemDrawer.tsx'

const TopicFilterManager: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { data, context, isLoading, isError } = useTopicFilterOperations()

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const columns = useMemo<ColumnDef<TopicFilter>[]>(() => {
    return [
      {
        accessorKey: 'topicFilter',
        header: t('topicFilter.listing.column.topicFilter'),
        cell: (info) => {
          return <Topic tagTitle={info.getValue<string>()} mr={3} />
        },
      },
      {
        accessorKey: 'description',
        header: t('topicFilter.listing.column.description'),
        cell: (info) => {
          return <Text as="span">{info.getValue<string>()}</Text>
        },
      },
      {
        id: 'actions',
        header: t('topicFilter.listing.column.action'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup role="toolbar">
              <ButtonGroup size="sm">
                <IconButton
                  aria-label={t('topicFilter.listing.action.edit.aria-label')}
                  icon={<LuView />}
                  onClick={() => console.log(info.row.index)}
                />
              </ButtonGroup>
              <ButtonGroup isAttached size="sm">
                <IconButton
                  aria-label={t('topicFilter.listing.action.edit.aria-label')}
                  icon={<LuPencil />}
                  onClick={() => console.log(info.row.index)}
                />
                <IconButton
                  aria-label={t('topicFilter.listing.action.delete.aria-label')}
                  icon={<LuTrash />}
                  onClick={() => console.log(info.row.index)}
                />
              </ButtonGroup>
            </ButtonGroup>
          )
        },
        footer: () => {
          return (
            <ArrayItemDrawer
              header={t('topicFilter.listing.title')}
              context={context}
              onSubmit={() => console.log('XXXXXXX')}
              trigger={({ onOpen: onOpenArrayDrawer }) => (
                <ButtonGroup isAttached size="sm">
                  <Button leftIcon={<LuPlus />} onClick={onOpenArrayDrawer}>
                    {t('topicFilter.listing.action.add.aria-label')}
                  </Button>
                </ButtonGroup>
              )}
            />
          )
        },
      },
    ]
  }, [context, t])

  return (
    <ExpandableDrawer
      header={t('topicFilter.manager.header')}
      isOpen={isOpen}
      onClose={handleClose}
      closeOnOverlayClick={false}
    >
      {isLoading && <LoaderSpinner />}
      {isError && <ErrorMessage message={t('topicFilter.error.loading')} />}
      {!isLoading && !isError && data?.items && (
        <Card size="sm">
          <CardBody>
            <PaginatedTable<TopicFilter>
              aria-label={t('topicFilter.listing.aria-label')}
              data={data.items}
              columns={columns}
              enablePagination={true}
            />
          </CardBody>
        </Card>
      )}
    </ExpandableDrawer>
  )
}

export default TopicFilterManager
