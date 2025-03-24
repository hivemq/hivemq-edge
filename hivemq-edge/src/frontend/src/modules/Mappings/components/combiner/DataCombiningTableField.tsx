import type { FC, ReactElement } from 'react'
import { useState } from 'react'
import { useMemo } from 'react'
import { v4 as uuidv4 } from 'uuid'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, HStack, Tag, TagLeftIcon, Text } from '@chakra-ui/react'
import type { FieldProps, RJSFSchema } from '@rjsf/utils'
import { LuPencil, LuPlus, LuTrash } from 'react-icons/lu'
import { FaKey } from 'react-icons/fa'

import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import IconButton from '@/components/Chakra/IconButton'
import { ConditionalWrapper } from '@/components/ConditonalWrapper'
import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag'
import DataCombiningEditorDrawer from './DataCombiningEditorDrawer'
import type { CombinerContext } from '../../types'

interface PrimaryWrapperProps {
  isPrimary: boolean
  children: ReactElement
}

const PrimaryWrapper: FC<PrimaryWrapperProps> = ({ children, isPrimary }) => {
  const { t } = useTranslation()

  return (
    <ConditionalWrapper
      condition={isPrimary}
      wrapper={(children) => (
        <Tag data-testid="primary-wrapper" role={'group'} p={1} variant="outline">
          <TagLeftIcon boxSize="12px" as={FaKey} ml={1} aria-label={t('combiner.schema.mapping.primary.aria-label')} />
          {children}
        </Tag>
      )}
    >
      {children}
    </ConditionalWrapper>
  )
}

export const DataCombiningTableField: FC<FieldProps<DataCombining[], RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)

  const handleOnSubmit = (data: DataCombining | undefined) => {
    if (selectedItem === undefined) return
    if (data) {
      if (!props.formData?.[selectedItem]) throw new Error(t('combiner.error.invalidData'))
      if (data.id !== props.formData?.[selectedItem].id) throw new Error(t('combiner.error.invalidData'))

      const newValues = [...props.formData]
      newValues[selectedItem] = data
      props.onChange(newValues)
    }
    setSelectedItem(undefined)
  }

  const displayColumns = useMemo<ColumnDef<DataCombining>[]>(() => {
    const handleAdd = () => {
      const newMapping: DataCombining = {
        id: uuidv4(),
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: [],
          topicFilters: [],
        },
        destination: { topic: '' },
        instructions: [],
      }
      props.onChange([...(props.formData || []), newMapping])
    }

    const handleDelete = (index: number) => {
      const newValues = [...(props.formData || [])]
      newValues.splice(index, 1)
      props.onChange(newValues)
    }

    return [
      {
        accessorKey: 'destination',
        cell: (info) => {
          if (info.row.original.destination.topic) return <Topic tagTitle={info.row.original.destination.topic} />
          return <Text>{t('combiner.unset')}</Text>
        },
      },
      {
        accessorKey: 'sources',
        cell: (info) => {
          const { sources } = info.row.original
          const nbItems = (sources?.tags?.length || 0) + (sources?.topicFilters?.length || 0)
          if (nbItems === 0) return <Text>{t('combiner.unset')}</Text>

          const primary = info.row.original.sources.primary

          const isPrimary = (type: DataIdentifierReference.type, id: string): boolean => {
            return primary.type === type && primary.id === id
          }

          return (
            <HStack flexWrap={'wrap'}>
              {info.row.original.sources?.tags?.map((tag) => (
                <PrimaryWrapper key={tag} isPrimary={Boolean(isPrimary(DataIdentifierReference.type.TAG, tag))}>
                  <PLCTag tagTitle={tag} />
                </PrimaryWrapper>
              ))}
              {info.row.original.sources?.topicFilters?.map((tag) => (
                <PrimaryWrapper
                  key={tag}
                  isPrimary={Boolean(isPrimary(DataIdentifierReference.type.TOPIC_FILTER, tag))}
                >
                  <TopicFilter tagTitle={tag} />
                </PrimaryWrapper>
              ))}
            </HStack>
          )
        },
      },

      {
        id: 'actions',
        header: t('combiner.schema.mappings.table.action'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup isAttached size="xs">
              <IconButton
                aria-label={t('combiner.schema.mappings.table.edit')}
                data-testid={'combiner-mapping-list-edit'}
                icon={<LuPencil />}
                onClick={() => setSelectedItem(info.row.index)}
              />
              <IconButton
                aria-label={t('combiner.schema.mappings.table.delete')}
                data-testid={'combiner-mapping-list-delete'}
                icon={<LuTrash />}
                onClick={() => handleDelete(info.row.index)}
              />{' '}
            </ButtonGroup>
          )
        },
        footer: () => {
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton
                data-testid={'combiner-mapping-list-add'}
                aria-label={t('combiner.schema.mappings.table.add')}
                icon={<LuPlus />}
                onClick={handleAdd}
              />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [props, t])

  return (
    <>
      <PaginatedTable<DataCombining>
        aria-label={t('combiner.schema.mappings.description')}
        data={props.formData || []}
        columns={displayColumns}
        enablePaginationGoTo={false}
        enablePaginationSizes={false}
        enableColumnFilters={false}
      />
      {selectedItem != undefined && props.formData?.[selectedItem] && (
        <DataCombiningEditorDrawer
          onClose={() => setSelectedItem(undefined)}
          onSubmit={handleOnSubmit}
          schema={props.schema.items as RJSFSchema}
          uiSchema={props.uiSchema?.items}
          formData={props.formData?.[selectedItem]}
          formContext={props.formContext}
        />
      )}
    </>
  )
}
