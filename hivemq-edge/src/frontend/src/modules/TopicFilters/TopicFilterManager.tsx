import { type FC, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { Button, ButtonGroup, Card, CardBody, Text, useDisclosure } from '@chakra-ui/react'
import { LuPencil, LuPlus, LuTrash, LuView } from 'react-icons/lu'

import { TopicFilter, TopicFilterList } from '@/api/__generated__'
import { useTopicFilterOperations } from '@/api/hooks/useTopicFilters/useTopicFilterOperations.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import ArrayItemDrawer from '@/components/rjsf/SplitArrayEditor/components/ArrayItemDrawer.tsx'
import TopicSchemaDrawer from '@/modules/TopicFilters/components/TopicSchemaDrawer.tsx'
import SchemaValidationMark from '@/modules/TopicFilters/components/SchemaValidationMark.tsx'

const TopicFilterManager: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { data, context, isLoading, isError, onUpdateCollection } = useTopicFilterOperations()

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
        id: 'hasSchema',
        header: t('topicFilter.listing.column.hasSchema'),
        cell: (info) => {
          return <SchemaValidationMark topicFilter={info.row.original} />
        },
      },
      {
        id: 'actions',
        header: t('topicFilter.listing.column.action'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup role="toolbar">
              <TopicSchemaDrawer
                topicFilter={info.row.original}
                trigger={({ onOpen: onOpenArrayDrawer }) => (
                  <ButtonGroup size="sm">
                    <IconButton
                      aria-label={t('topicFilter.listing.action.view.aria-label')}
                      icon={<LuView />}
                      onClick={onOpenArrayDrawer}
                    />
                  </ButtonGroup>
                )}
              />

              <ButtonGroup isAttached size="sm" isDisabled>
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
              header={t('topicFilter.listing.aria-label')}
              context={context}
              onSubmit={(w) => {
                onUpdateCollection(w as TopicFilterList)
              }}
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
  }, [context, onUpdateCollection, t])

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
